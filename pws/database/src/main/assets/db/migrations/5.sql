-- create table tags
-- column predefined: 1 for predefined, 0 for custom
CREATE TABLE IF NOT EXISTS tags (
    _id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    color TEXT NOT NULL,
    predefined INTEGER NOT NULL DEFAULT 0
);

-- create table psalm_tag
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS psalm_tag (
    psalmnumbers_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (psalmnumbers_id, tag_id),
    FOREIGN KEY (psalmnumbers_id) REFERENCES psalmnumbers(_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(_id) ON DELETE CASCADE
);

-- Initializes predefined tags for song categorization in the 'tag' table
BEGIN TRANSACTION;

INSERT INTO tags (_id, name, color, predefined) VALUES
(1,'Перед Началом Собрания', '#D3D3D3', 1),
(2 ,'Молитвенные', '#D3D3D3', 1),
(3 ,'Божья Любовь и Величие', '#D3D3D3', 1),
(4 ,'Хвала и Благодарение', '#D3D3D3', 1),
(5 ,'Христианская Радость', '#D3D3D3', 1),
(6 ,'Путь Веры', '#D3D3D3', 1),
(7 ,'О Церкви', '#D3D3D3', 1),
(8 ,'Призыв к Труду', '#D3D3D3', 1),
(9 ,'Призыв к Покаянию', '#D3D3D3', 1),
(10 ,'Для Новообращённых', '#D3D3D3', 1),
(11 ,'На Крещение', '#D3D3D3', 1),
(12 ,'На Хлебопреломление', '#D3D3D3', 1),
(13 ,'Страдание и Смерть Христа', '#D3D3D3', 1),
(14 ,'На Рукоположение', '#D3D3D3', 1),
(15 ,'На Бракосочетание', '#D3D3D3', 1),
(16 ,'Для Детей и Семейные', '#D3D3D3', 1),
(17 ,'На Погребение', '#D3D3D3', 1),
(18 ,'Небесные Обители', '#D3D3D3', 1),
(19 ,'Утешение Больным, Страждущим', '#D3D3D3', 1),
(20 ,'Рождество Христово', '#D3D3D3', 1),
(21 ,'На Новый Год', '#D3D3D3', 1),
(22 ,'Воскресение Христово', '#D3D3D3', 1),
(23 ,'О Духе Святом', '#D3D3D3', 1),
(24 ,'Жатвенные', '#D3D3D3', 1),
(25 ,'Второе Пришествие Христа', '#D3D3D3', 1),
(26 ,'Приветственные и Прощальные', '#D3D3D3', 1),
(27 ,'При Заключении Собрания', '#D3D3D3', 1),
(28 ,'Молодёжные', '#D3D3D3', 1),
(1000, 'Вдохновляющие', '#D3D3D3', 1);

COMMIT;

