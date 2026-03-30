package com.example.mainbackend.algorithm.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoConstraintRequest {
    private Long predecessorId;

    /** * Type of constraint: FS, SS, FF, SF.
     * Default: FS
     */
    private ConstraintType type;
}
