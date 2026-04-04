package de.jupiter1202.gymtracker.feature.plans

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateParserTest {

    companion object {
        // Inline copy of app/src/test/assets/templates.json — avoids file I/O in unit tests
        private const val FIXTURE_JSON = """[
  {
    "id": "ppl",
    "name": "Push Pull Legs",
    "description": "Push Pull Legs — 3 days/week. Classic hypertrophy split.",
    "days": [
      {
        "name": "Push",
        "exercises": [
          { "exercise_name": "Bench Press", "target_sets": 4, "target_reps": "8-12" }
        ]
      }
    ]
  },
  {
    "id": "stronglifts_5x5",
    "name": "StrongLifts 5x5",
    "description": "StrongLifts 5x5 — 3 days/week. Beginner strength program.",
    "days": [
      {
        "name": "Workout A",
        "exercises": [
          { "exercise_name": "Squat", "target_sets": 5, "target_reps": "5" }
        ]
      }
    ]
  }
]"""
    }

    private val json = Json { ignoreUnknownKeys = true }

    // PLAN-02: JSON fixture has exactly 2 programs
    @Test
    fun parseTemplates_returnsTwoPrograms() {
        val programs = json.decodeFromString<List<TemplateProgram>>(FIXTURE_JSON)
        assertEquals("Expected 2 programs", 2, programs.size)
    }

    // PLAN-02: First program is PPL
    @Test
    fun parseTemplates_firstProgramIsPPL() {
        val programs = json.decodeFromString<List<TemplateProgram>>(FIXTURE_JSON)
        assertEquals("Expected first program id to be 'ppl'", "ppl", programs[0].id)
    }

    // PLAN-02: Second program is StrongLifts 5x5
    @Test
    fun parseTemplates_secondProgramIs5x5() {
        val programs = json.decodeFromString<List<TemplateProgram>>(FIXTURE_JSON)
        assertEquals(
            "Expected second program id to be 'stronglifts_5x5'",
            "stronglifts_5x5",
            programs[1].id
        )
    }
}
