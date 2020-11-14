package org.cubeengine.module.locker.old.door;

import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

public class DoorConfig extends ReflectedYaml
{
    @Name("settings.open-iron-door-with-click")
    public boolean openIronDoorWithClick = false;

    @Comment("If set to true protected doors will auto-close after the configured time")
    @Name("settings.auto-close.enable")
    public boolean autoCloseEnable = true;

    @Comment("Doors will auto-close after this set amount of seconds.")
    @Name("settings.auto-close.time")
    public int autoCloseSeconds = 3;

}
