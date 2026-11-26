#Requires -Version 5.1
<#
.SYNOPSIS
  Helper (dot-source) usado pelos scripts run-*.ps1 para montar a chamada do k6.

.DESCRIPTION
  Por padrao o k6 roda como conteiner Linux (grafana/k6) DENTRO da rede Docker
  do compose testado, acessando os servicos pelo nome (http://read-app:8081)
  em vez de localhost. Motivos:

    - o kernel Linux lida melhor com milhares de conexoes simultaneas do que
      a pilha TCP do Windows (portas efemeras, TIME_WAIT);
    - elimina o encaminhamento de portas do Docker Desktop (host Windows ->
      VM WSL2), que tambem consome conexoes e adiciona latencia;
    - o gerador de carga fica isolado em seu proprio conteiner
      (Jiang e Hassan, 2015, secao 4.1.2).

  Com -Local o comportamento antigo e mantido (k6.exe no Windows + localhost).

  Uso:
    . (Join-Path $ScriptDir "k6\k6-docker.ps1")
    $k6 = Get-K6Invocation -Project data-replication -TestFile $testFile -InfluxDB $InfluxDB
    & $k6.Exe @($k6.Args)
#>

# Mesma versao do k6.exe usado nas rodadas anteriores, para manter comparabilidade.
$script:K6Image = "grafana/k6:1.7.1"

# Rede criada pelo docker compose de cada projeto (<pasta>_<rede>) e nomes dos servicos.
$script:K6Targets = @{
    "data-replication" = @{
        Network  = "cqrs-data-replication_crm-network"
        Staff    = "http://staff-app:8082"
        Customer = "http://customer-app:8083"
        Wallet   = "http://wallet-app:8084"
        Read     = "http://read-app:8081"
    }
    "event-sourcing" = @{
        Network  = "cqrs-event-sourcing_default"
        Staff    = "http://staff:8082"
        Customer = "http://customer:8083"
        Wallet   = "http://wallet:8084"
        Read     = "http://read:8081"
    }
}

function Test-K6DockerNetwork {
    param([Parameter(Mandatory)][ValidateSet("data-replication", "event-sourcing")][string]$Project)
    $network = $script:K6Targets[$Project].Network
    & docker network inspect $network *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Rede Docker '$network' nao encontrada. Suba o compose do projeto (cqrs-$Project) antes, ou use -LocalK6." -ForegroundColor Red
        return $false
    }
    return $true
}

function Get-K6Invocation {
    param(
        [Parameter(Mandatory)][ValidateSet("data-replication", "event-sourcing")][string]$Project,
        [Parameter(Mandatory)][string]$TestFile,
        # Vazio = sem --out influxdb.
        [string]$InfluxDB = "",
        # Nome do conteiner (permite matar com 'docker rm -f' em caso de timeout).
        [string]$ContainerName = "",
        # Variaveis extras para o script, no formato "NOME=valor" (ex.: "N=10000").
        [string[]]$ExtraEnv = @(),
        [switch]$Local
    )

    $scriptsDir = Split-Path -Parent (Resolve-Path $TestFile).Path
    $fileName   = Split-Path -Leaf $TestFile
    # Pasta data/ do projeto (carteiras do teste de escrita): unica parte gravavel.
    $dataDir    = Join-Path $scriptsDir "data"
    $hasDataDir = Test-Path $dataDir

    if ($Local) {
        $k6Args = @("run")
        if ($InfluxDB) { $k6Args += @("--out", "influxdb=$InfluxDB") }
        if ($hasDataDir) { $k6Args += @("-e", "DATA_DIR=$dataDir") }
        foreach ($e in $ExtraEnv) { $k6Args += @("-e", $e) }
        $k6Args += $TestFile
        return [PSCustomObject]@{ Exe = "k6"; Args = $k6Args; ContainerName = $null }
    }

    $target = $script:K6Targets[$Project]
    if (-not $ContainerName) { $ContainerName = "k6-" + [guid]::NewGuid().ToString("N").Substring(0, 12) }

    $dockerArgs = @(
        "run", "--rm",
        "--name", $ContainerName,
        "--network", $target.Network,
        # Limites de rede/arquivos para milhares de VUs (sysctls com namespace por conteiner).
        "--ulimit", "nofile=1048576:1048576",
        "--sysctl", "net.ipv4.ip_local_port_range=1024 65535",
        "--sysctl", "net.ipv4.tcp_tw_reuse=1",
        "-v", "${scriptsDir}:/scripts:ro"
    )
    if ($hasDataDir) { $dockerArgs += @("-v", "${dataDir}:/scripts/data") }
    $dockerArgs += @(
        $script:K6Image,
        "run",
        "-e", "STAFF_URL=$($target.Staff)",
        "-e", "CUSTOMER_URL=$($target.Customer)",
        "-e", "WALLET_URL=$($target.Wallet)",
        "-e", "READ_URL=$($target.Read)"
    )
    if ($hasDataDir) { $dockerArgs += @("-e", "DATA_DIR=/scripts/data") }
    foreach ($e in $ExtraEnv) { $dockerArgs += @("-e", $e) }
    if ($InfluxDB) {
        # O InfluxDB esta em outro compose; dentro do conteiner 'localhost' nao e o host.
        $influxInContainer = $InfluxDB -replace "//(localhost|127\.0\.0\.1)", "//host.docker.internal"
        $dockerArgs += @("--out", "influxdb=$influxInContainer")
    }
    $dockerArgs += "/scripts/$fileName"

    return [PSCustomObject]@{ Exe = "docker"; Args = $dockerArgs; ContainerName = $ContainerName }
}

# Preparacao do teste de escrita: roda 00-write-seed.js com N carteiras e grava
# data/write-wallets-<N>.json. Fica fora da execucao da carga para que o k6 nao
# copie os IDs para cada VU e para que o setup nao entre nas metricas.
# Retorna quantas carteiras foram gravadas (0 em caso de falha).
function Invoke-K6WriteSeed {
    param(
        [Parameter(Mandatory)][ValidateSet("data-replication", "event-sourcing")][string]$Project,
        [Parameter(Mandatory)][string]$K6Dir,
        [Parameter(Mandatory)][int]$N,
        [Parameter(Mandatory)][string]$LogFile,
        [switch]$Local
    )
    $dataDir = Join-Path $K6Dir "data"
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    $walletsFile = Join-Path $dataDir "write-wallets-$N.json"
    Remove-Item -Path $walletsFile -Force -ErrorAction SilentlyContinue

    $k6 = Get-K6Invocation -Project $Project -TestFile (Join-Path $K6Dir "00-write-seed.js") -ExtraEnv @("N=$N") -Local:$Local
    & $k6.Exe @($k6.Args) 2>&1 | ForEach-Object { "$_" } | Out-File -FilePath $LogFile -Encoding utf8

    if (-not (Test-Path $walletsFile)) { return 0 }
    $wallets = Get-Content $walletsFile -Raw | ConvertFrom-Json
    return $wallets.Count
}
