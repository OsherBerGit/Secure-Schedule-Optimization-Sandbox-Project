package com.example.mainbackend.service;

import com.example.mainbackend.dto.skill.SkillDto;
import com.example.mainbackend.entity.Skill;
import com.example.mainbackend.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Skill getSkillById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + id));
    }

    @Transactional
    public Skill createSkill(SkillDto dto) {
        if (skillRepository.findByName(dto.getName()).isPresent())
            throw new IllegalArgumentException("Skill already exists: " + dto.getName());

        Skill skill = Skill.builder()
                .name(dto.getName())
                .build();

        return skillRepository.save(skill);
    }

    @Transactional
    public Skill updateSkill(Long id, SkillDto dto) {
        Skill skill = getSkillById(id);
        if (!skill.getName().equals(dto.getName()) && skillRepository.findByName(dto.getName()).isPresent())
            throw new IllegalArgumentException("Skill with this name already exists");

        skill.setName(dto.getName());
        return skillRepository.save(skill);
    }

    @Transactional
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id))
            throw new IllegalArgumentException("Skill title not found: " + id);
        skillRepository.deleteById(id);
    }
}

