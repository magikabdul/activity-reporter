-- Create table for LUFA tasks
-- Columns:
-- - id: primary key
-- - created_at: DATE of the task (required)
-- - category: TaskCategory enum stored as VARCHAR with CHECK constraint (required)
-- - description: description of the performed task (required)

CREATE TABLE IF NOT EXISTS lufthansa (
    id BIGSERIAL PRIMARY KEY,
    created_at DATE NOT NULL,
    category VARCHAR(64) NOT NULL CHECK (
        category IN (
            'SOFTWARE_DEVELOPMENT',
            'CONSULTING_AND_TRAINING',
            'DOCUMENTATION',
            'CODE_ANALYSIS_AND_REFINEMENT',
            'BUG_FIXING_AND_MAINTENANCE',
            'TECHNOLOGY_SELECTION',
            'ARCHITECTURE_DESIGN'
        )
    ),
    description TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_lufa_created_at ON lufthansa (created_at);
CREATE INDEX IF NOT EXISTS idx_lufa_category ON lufthansa (category);
