package com.example.sidebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConstraintType {
    @JsonProperty("FS") FS,
    @JsonProperty("SS") SS,
    @JsonProperty("FF") FF,
    @JsonProperty("SF") SF;
}
