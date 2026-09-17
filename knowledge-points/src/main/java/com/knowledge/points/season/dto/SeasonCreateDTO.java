package com.knowledge.points.season.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SeasonCreateDTO(
        @NotBlank @Pattern(regexp = "\\d{4}-Q[1-4]") String season,
        @NotBlank @Size(max = 100) String name) {
}
