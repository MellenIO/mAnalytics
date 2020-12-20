-- Set up tables
create table if not exists entity
(
    entity_type_id   int auto_increment
        primary key,
    entity_type_name varchar(32) not null,
    constraint entity_entity_type_name_uindex
        unique (entity_type_name)
)
    comment 'Entity Definition Table';

create table if not exists entity_attribute
(
    attribute_id   int auto_increment
        primary key,
    entity_type_id int                                            not null,
    attribute_name varchar(32)                                    not null,
    attribute_type enum ('varchar', 'int', 'decimal', 'datetime') not null,
    constraint entity_attribute_entity_entity_type_id_fk
        foreign key (entity_type_id) references entity (entity_type_id)
)
    comment 'Entity Attribute Definition Table';

create table if not exists entity_attribute_datetime
(
    row_id         int auto_increment
        primary key,
    attribute_id   int       null,
    entity_id      int       null,
    value          timestamp null,
    entity_type_id int       not null,
    constraint entity_attribute_datetime_eid
        unique (attribute_id, entity_id, entity_type_id),
    constraint entity_attribute_datetime_entity_attribute_attribute_id_fk
        foreign key (attribute_id) references entity_attribute (attribute_id),
    constraint entity_attribute_datetime_entity_entity_type_id_fk
        foreign key (entity_type_id) references entity (entity_type_id)
)
    comment 'EAV Table for datetime';

create table if not exists entity_attribute_decimal
(
    row_id         int auto_increment
        primary key,
    attribute_id   int     null,
    entity_id      int     null,
    value          decimal null,
    entity_type_id int     not null,
    constraint entity_attribute_decimal_eid
        unique (attribute_id, entity_id, entity_type_id),
    constraint entity_attribute_decimal_entity_attribute_attribute_id_fk
        foreign key (attribute_id) references entity_attribute (attribute_id),
    constraint entity_attribute_decimal_entity_entity_type_id_fk
        foreign key (entity_type_id) references entity (entity_type_id)
)
    comment 'EAV Table for decimal';

create table if not exists entity_attribute_int
(
    row_id         int auto_increment
        primary key,
    attribute_id   int null,
    entity_id      int null,
    value          int null,
    entity_type_id int not null,
    constraint entity_attribute_int_eid
        unique (attribute_id, entity_id, entity_type_id),
    constraint entity_attribute_int_entity_attribute_attribute_id_fk
        foreign key (attribute_id) references entity_attribute (attribute_id),
    constraint entity_attribute_int_entity_entity_type_id_fk
        foreign key (entity_type_id) references entity (entity_type_id)
)
    comment 'EAV Table for int';

create table if not exists entity_attribute_varchar
(
    row_id         int auto_increment
        primary key,
    attribute_id   int          null,
    entity_id      int          null,
    value          varchar(255) null,
    entity_type_id int          not null,
    constraint entity_attribute_varchar_eid
        unique (attribute_id, entity_id, entity_type_id),
    constraint entity_attribute_varchar_entity_attribute_attribute_id_fk
        foreign key (attribute_id) references entity_attribute (attribute_id),
    constraint entity_attribute_varchar_entity_entity_type_id_fk
        foreign key (entity_type_id) references entity (entity_type_id)
)
    comment 'EAV Table for varchar';

create table if not exists player
(
    entity_id  int auto_increment
        primary key,
    uuid       varchar(36)                         not null,
    created_at timestamp default CURRENT_TIMESTAMP not null,
    updated_at timestamp default CURRENT_TIMESTAMP not null,
    name       varchar(16)                         not null
)
    comment 'Main Player Table';

create table if not exists player_event
(
    event_id     bigint auto_increment
        primary key,
    event_name   varchar(64)                         not null,
    entity_id    int                                 not null,
    custom_one   varchar(255)                        null,
    custom_two   varchar(255)                        null,
    custom_three varchar(255)                        null,
    custom_four  varchar(255)                        null,
    created_at   timestamp default CURRENT_TIMESTAMP not null,
    constraint player_event_player_entity_id_fk
        foreign key (entity_id) references player (entity_id)
);

create table if not exists player_name_history
(
    history_id int auto_increment
        primary key,
    entity_id  int                                 null,
    old_name   varchar(16)                         null,
    new_name   varchar(16)                         null,
    created_at timestamp default CURRENT_TIMESTAMP not null,
    constraint player_name_history_player_entity_id_fk
        foreign key (entity_id) references player (entity_id)
)
    comment 'Player Name History';

create table if not exists player_session_history
(
    session_id  int auto_increment
        primary key,
    entity_id   int                                 not null,
    started_at  timestamp default CURRENT_TIMESTAMP not null,
    finished_at timestamp                           null,
    constraint player_session_history_player_entity_id_fk
        foreign key (entity_id) references player (entity_id)
)
    comment 'Player Session History';

-- Initial data set up
-- NOTE: we use insert ignore here to make the script repeatable
insert ignore into entity(entity_type_name) values ('player'), ('player_event');

-- Trigger to amend the name history table when a players name has been updated
delimiter //
create trigger update_name_history after update on player
    for each row
    begin
        if !(new.name <=> old.name) then
            insert into player_name_history (entity_id, old_name, new_name)
                values (
                        new.entity_id,
                        old.name,
                        new.name
                       );
        end if;
    end //
delimiter ;