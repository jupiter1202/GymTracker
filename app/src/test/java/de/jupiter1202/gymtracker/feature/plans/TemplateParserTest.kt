package de.jupiter1202.gymtracker.feature.plans

import org.junit.Assert.assertEquals
import org.junit.Test

// ---------------------------------------------------------------------------
// Stub types — replaced by real kotlinx.serialization types in plan 03-02/03-03
// ---------------------------------------------------------------------------

data class TemplateProgram(
    val id: String,
    val name: String,
    val description: String
)

/**
 * Minimal stub JSON parser — no Android or external dependency needed.
 * Extracts top-level objects and their "id", "name", "description" string fields
 * using a simple regex approach. Replaced by kotlinx.serialization in plan 03-03.
 */
fun parseTemplates(json: String): List<TemplateProgram> {
    val objectPattern = Regex("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", RegexOption.DOT_MATCHES_ALL)
    val fieldPattern = Regex("\"(id|name|description)\"\\s*:\\s*\"([^\"]+)\"")

    // Split on top-level objects: find the outer array items
    // We walk depth-manually to split by top-level `{...}` blocks
    val topLevel = mutableListOf<String>()
    var depth = 0
    var start = -1
    for (i in json.indices) {
        when (json[i]) {
            '{' -> {
                if (depth == 0) start = i
                depth++
            }
            '}' -> {
                depth--
                if (depth == 0 && start >= 0) {
                    topLevel.add(json.substring(start, i + 1))
                    start = -1
                }
            }
        }
    }

    return topLevel.map { obj ->
        val fields = fieldPattern.findAll(obj).associate { it.groupValues[1] to it.groupValues[2] }
        TemplateProgram(
            id = fields["id"] ?: "",
            name = fields["name"] ?: "",
            description = fields["description"] ?: ""
        )
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

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

    // PLAN-02: JSON fixture has exactly 2 programs
    @Test
    fun parseTemplates_returnsTwoPrograms() {
        val programs = parseTemplates(FIXTURE_JSON)
        assertEquals("Expected 2 programs", 2, programs.size)
    }

    // PLAN-02: First program is PPL
    @Test
    fun parseTemplates_firstProgramIsPPL() {
        val programs = parseTemplates(FIXTURE_JSON)
        assertEquals("Expected first program id to be 'ppl'", "ppl", programs[0].id)
    }

    // PLAN-02: Second program is StrongLifts 5x5
    @Test
    fun parseTemplates_secondProgramIs5x5() {
        val programs = parseTemplates(FIXTURE_JSON)
        assertEquals(
            "Expected second program id to be 'stronglifts_5x5'",
            "stronglifts_5x5",
            programs[1].id
        )
    }
}
