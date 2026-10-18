package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The set of filter values that actually have questions, so the game setup
 * screen only offers combinations that will not come back empty.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizFacets {

    private List<String> categories;
    private List<Integer> difficulties;
    private List<String> languages;
}
