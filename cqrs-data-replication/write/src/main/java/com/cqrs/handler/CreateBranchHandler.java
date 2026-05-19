package com.cqrs.handler;

import com.cqrs.domain.command.CreateBranchCommand;
import com.cqrs.entity.BranchEntity;
import com.cqrs.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBranchHandler {

    private final BranchRepository branchRepository;

    public UUID handle(CreateBranchCommand cmd) {
        BranchEntity entity = new BranchEntity();
        entity.setId(UUID.randomUUID());
        entity.setRegion(cmd.region());
        entity.setCreatedAt(Instant.now());
        branchRepository.save(entity);
        return entity.getId();
    }
}
