MERGE INTO app_user (
    username,
    password_hash,
    email,
    total_points,
    display_name,
    profile_icon_color,
    enabled
)
KEY (username)
VALUES (
    'b-boy49',
    '$2y$10$bccIE5cdr.UEM8t8LmjAiO.SINceQrfXAkQuLfjBtSIrhWz/NvK/2',
    'bboy49@example.local',
    0,
    'b-boy49',
    '#BFD6FF',
    TRUE
);
