package com.example.mainbackend.service;

import com.example.mainbackend.dto.job.JobDto;
import com.example.mainbackend.entity.Job;
import com.example.mainbackend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    @Transactional
    public Job createJob(JobDto dto) {
        if (jobRepository.findByName(dto.getName()).isPresent())
            throw new IllegalArgumentException("Job already exists: " + dto.getName());

        Job job = Job.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        return jobRepository.save(job);
    }

    @Transactional
    public Job updateJob(Long id, JobDto dto) {
        Job job = getJobById(id);
        if (!job.getName().equals(dto.getName()) && jobRepository.findByName(dto.getName()).isPresent())
            throw new IllegalArgumentException("Job with this name already exists");

        job.setName(dto.getName());
        job.setDescription(dto.getDescription());
        return jobRepository.save(job);
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id))
            throw new IllegalArgumentException("Job title not found: " + id);
        jobRepository.deleteById(id);
    }
}

