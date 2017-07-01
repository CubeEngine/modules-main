/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.vanillaplus.improvement.summon;

import org.cubeengine.libcube.service.logging.LogProvider;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.data.type.OcelotTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Ageable;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.animal.Horse;
import org.spongepowered.api.entity.living.animal.Ocelot;
import org.spongepowered.api.entity.living.animal.Pig;
import org.spongepowered.api.entity.living.animal.Sheep;
import org.spongepowered.api.entity.living.animal.Wolf;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.monster.Enderman;
import org.spongepowered.api.entity.living.monster.Slime;
import org.spongepowered.api.entity.living.monster.Zombie;
import org.spongepowered.api.entity.living.monster.ZombiePigman;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class EntityDataChanger<EntityInterface>
{
    private final Class<EntityInterface> clazz;
    protected final EntityChanger<EntityInterface, ?> changer;
    public static final Set<EntityDataChanger> entityDataChangers = new HashSet<>();

    public static final EntityDataChanger<Pig> PIG_SADDLE =
            new EntityDataChanger<>(Pig.class,
                new BoolEntityChanger<Pig>("saddled")
                {
                    @Override
                    public void applyEntity(Pig entity, Boolean input)
                    {
                        entity.offer(Keys.PIG_SADDLE, input);
                    }
                });

    public static final EntityDataChanger<Horse> HORSE_SADDLE =
        new EntityDataChanger<>(Horse.class,
                                new BoolEntityChanger<Horse>("saddled")
                                {
                                    @Override
                                    public void applyEntity(Horse entity, Boolean input)
                                    {
                                        entity.offer(Keys.PIG_SADDLE, input);
                                    }
                                });

    public static final EntityDataChanger<Ageable> AGEABLE_BABY =
            new EntityDataChanger<>(Ageable.class,
                    new BoolEntityChanger<Ageable>("baby") {
                        @Override
                        public void applyEntity(Ageable entity, Boolean input)
                        {
                            // TODO how is this set?
                            if (input)
                            {
                                entity.offer(Keys.AGE, 0);
                            }
                            else
                            {
                                entity.offer(Keys.AGE, 1);
                            }
                        }
                    });

    public static final EntityDataChanger<Zombie> ZOMBIE_VILLAGER =
        new EntityDataChanger<>(Zombie.class,
          new BoolEntityChanger<Zombie>("villager") {
              @Override
              public void applyEntity(Zombie entity, Boolean input) {
                  // TODO how is this set?
                  if (input)
                  {
                      entity.offer(Keys.AGE, 0);
                  }
                  else
                  {
                      entity.offer(Keys.AGE, 1);
                  }
              }
          });

    public static final EntityDataChanger<Wolf> WOLF_ANGRY =
            new EntityDataChanger<>(Wolf.class,
                    new BoolEntityChanger<Wolf>("angry") {
                        @Override
                        public void applyEntity(Wolf entity, Boolean input) {
                            entity.offer(Keys.ANGRY, input);
                        }
                    });

    public static final EntityDataChanger<ZombiePigman> PIGZOMBIE_ANGRY =
        new EntityDataChanger<>(ZombiePigman.class,
                          new BoolEntityChanger<ZombiePigman>("angry") {
                              @Override
                              public void applyEntity(ZombiePigman entity, Boolean input) {
                                  entity.offer(Keys.ANGRY, input);
                              }
                          });

    public static final EntityDataChanger<Creeper> CREEPER_POWERED =
            new EntityDataChanger<>(Creeper.class,
                    new BoolEntityChanger<Creeper>("powered", "power", "charged") {
                        @Override
                        public void applyEntity(Creeper entity, Boolean input) {
                            entity.offer(Keys.CREEPER_CHARGED, input);
                        }
                    });

    public static final EntityDataChanger<Wolf> WOLF_SIT =
            new EntityDataChanger<>(Wolf.class,
                    new BoolEntityChanger<Wolf>("sitting", "sit") {
                        @Override
                        public void applyEntity(Wolf entity, Boolean input) {
                            entity.offer(Keys.IS_SITTING, input);
                        }
                    });

    public static final EntityDataChanger<Ocelot> OCELOT_SIT =
        new EntityDataChanger<>(Ocelot.class,
         new BoolEntityChanger<Ocelot>("sitting", "sit") {
             @Override
             public void applyEntity(Ocelot entity, Boolean input) {
                 entity.offer(Keys.IS_SITTING, input);
             }
         });

    /*
    public static final EntityDataChanger<Skeleton> SKELETON_TYPE =
        new EntityDataChanger<>(Skeleton.class,
         new BoolEntityChanger<Skeleton>("wither") {
             @Override
             public void applyEntity(Skeleton entity, Boolean input)
             {
                 entity.offer(Keys.SKELETON_TYPE, input ? SkeletonTypes.WITHER : SkeletonTypes.NORMAL);
             }
         });
     */

    public static final EntityDataChanger<Sheep> SHEEP_SHEARED =
        new EntityDataChanger<>(Sheep.class,
                     new BoolEntityChanger<Sheep>("sheared") {
                         @Override
                         public void applyEntity(Sheep entity, Boolean input)
                         {
                             entity.offer(Keys.IS_SHEARED, input);
                         }
                     });


    public static final EntityDataChanger<Ocelot> OCELOT_TYPE =
        new EntityDataChanger<>(Ocelot.class,
              new MappedEntityChanger<Ocelot, OcelotType>() {
                  @Override
                  void fillValues()
                  {
                      this.map.put("black", OcelotTypes.BLACK_CAT);
                      this.map.put("red", OcelotTypes.RED_CAT);
                      this.map.put("orange", OcelotTypes.RED_CAT);
                      this.map.put("white", OcelotTypes.SIAMESE_CAT);
                      this.map.put("siamese", OcelotTypes.SIAMESE_CAT);
                  }

                  @Override
                  public void applyEntity(Ocelot entity, OcelotType input)
                  {
                      entity.offer(Keys.OCELOT_TYPE, input);
                  }
              });

    public static  final EntityDataChanger<Sheep> SHEEP_COLOR =
            new EntityDataChanger<>(Sheep.class,
                    new EntityChanger<Sheep, DyeColor>() {
                        @Override
                        public void applyEntity(Sheep entity, DyeColor input)
                        {
                            entity.offer(Keys.DYE_COLOR, input);
                        }
                        @Override
                        public DyeColor getTypeValue(String input)
                        {
                            for (DyeColor color : Sponge.getRegistry().getAllOf(DyeColor.class))
                            {
                                if (color.getName().equals(input))
                                {
                                    return color;
                                }
                            }
                            return null;
                        }
                    });

    public static  final EntityDataChanger<Sheep> SHEEP_COLOR_RANDOM =
        new EntityDataChanger<>(Sheep.class,
                 new BoolEntityChanger<Sheep>("random") {
                     private final Random random = new Random(System.nanoTime());
                     @Override
                     public void applyEntity(Sheep entity, Boolean input)
                     {
                         if (input)
                         {
                             ArrayList<DyeColor> list = new ArrayList<>(Sponge.getRegistry().getAllOf(DyeColor.class));
                             entity.offer(Keys.DYE_COLOR, list.get(random.nextInt(list.size())));
                         }
                     }
                 });


    public static final EntityDataChanger<Wolf> WOLF_COLLAR =
        new EntityDataChanger<>(Wolf.class,
                   new EntityChanger<Wolf, DyeColor>() {
                         @Override
                         public void applyEntity(Wolf entity, DyeColor input) {
                             entity.offer(Keys.DYE_COLOR, input);
                         }
                         @Override
                         public DyeColor getTypeValue(String input)
                         {
                             for (DyeColor color : Sponge.getRegistry().getAllOf(DyeColor.class))
                             {
                                 if (color.getName().equals(input))
                                 {
                                     return color;
                                 }
                             }
                             return null;
                         }
                     });

    public static  final EntityDataChanger<Villager> VILLAGER_PROFESSION =
            new EntityDataChanger<>(Villager.class,
                    new EntityChanger<Villager, Career>() {
                        @Override
                        public void applyEntity(Villager entity, Career input)
                        {
                            entity.offer(Keys.CAREER, input);
                        }

                        @Override
                        public Career getTypeValue(String input)
                        {
                            for (Career career : Sponge.getRegistry().getAllOf(Career.class))
                            {
                                if (career.getName().equals(input))
                                {
                                    return career;
                                }
                            }
                            return null;
                        }
                    });

    public static final EntityDataChanger<Enderman> ENDERMAN_ITEM =
            new EntityDataChanger<>(Enderman.class,
                    new EntityChanger<Enderman, ItemStack>() {
                        @Override
                        public void applyEntity(Enderman entity, ItemStack value)
                        {
                            // TODO Enderman item
                        }

                        @Override
                        public ItemStack getTypeValue(String input)
                        {
                            for (ItemType item : Sponge.getRegistry().getAllOf(ItemType.class))
                            {
                                if (item.getName().equals(input))
                                {
                                    return ItemStack.of(item, 1);
                                }
                            }
                            return null;
                        }
                    });

    public static final EntityDataChanger<Slime> SLIME_SIZE =
        new EntityDataChanger<>(Slime.class,
                                             new EntityChanger<Slime,Integer>() {
                                                 @Override
                                                 public void applyEntity(Slime entity, Integer input)
                                                 {
                                                     entity.offer(Keys.SLIME_SIZE, input);
                                                 }

                                                 private StringMatcher sm = new StringMatcher(null); // TODO this is suboptimal

                                                 @Override
                                                 public Integer getTypeValue(String input)
                                                 {
                                                     String match = sm.matchString(input, "tiny", "small", "big");
                                                     if (match != null)
                                                     {
                                                         switch (match)
                                                         {
                                                             case "tiny":
                                                                 return 0;
                                                             case "small":
                                                                 return 2;
                                                             case "big":
                                                                 return 4;
                                                         }
                                                     }
                                                     try
                                                     {
                                                         Integer parsed = Integer.parseInt(input);
                                                         return (parsed > 0 && parsed <= 250) ? parsed : null;
                                                     }
                                                     catch (NumberFormatException ex)
                                                     {
                                                         return null;
                                                     }
                                                 }
                                             });

    public static final EntityDataChanger<Living> HP =
            new EntityDataChanger<>(Living.class,
                    new EntityChanger<Living, Integer>() {
                        @Override
                        public void applyEntity(Living entity, Integer input)
                        {
                            entity.offer(Keys.MAX_HEALTH, input.doubleValue());
                            entity.offer(Keys.HEALTH, input.doubleValue());
                        }

                        @Override
                        public Integer getTypeValue(String input)
                        {
                            if (input.endsWith("hp"))
                            {
                                try
                                {
                                    return Integer.parseInt(input.substring(0,input.length()-2));
                                }
                                catch (NumberFormatException ignored)
                                {}
                            }
                            return null;
                        }
                    });

    public static final EntityDataChanger<Horse> HORSE_JUMP =
        new EntityDataChanger<>(Horse.class,
                                new EntityChanger<Horse, Integer>() {
                                    @Override
                                    public void applyEntity(Horse entity, Integer input)
                                    {
                                        // TODO jumpstrength
                                    }

                                    @Override
                                    public Integer getTypeValue(String input)
                                    {
                                        if (input.startsWith("jump"))
                                        {
                                            try
                                            {
                                                int jump = Integer.parseInt(input.substring(4, input.length()));
                                                if (jump >= 0 && jump <= 2)
                                                {
                                                    return jump;
                                                }
                                            }
                                            catch (NumberFormatException ignored)
                                            {}
                                        }
                                        return null;
                                    }
                                });

    // TODO EntitySpeed using Bukkit-API #WaitForBukkit
    public static final EntityDataChanger<Living> ENTITY_SPEED =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, Double>() {
                                    @Override
                                    public void applyEntity(Living entity, Double input)
                                    {
                                        // TODO set Entity Speed
                                    }

                                    @Override
                                    public Double getTypeValue(String input)
                                    {
                                        if (input.startsWith("speed"))
                                        {
                                            try
                                            {
                                                double speed = Double.parseDouble(input.substring(5, input.length()));
                                                if (speed >= 0 && speed <= 2)
                                                {
                                                    return speed;
                                                }
                                            }
                                            catch (NumberFormatException ignored)
                                            {}
                                        }
                                        return null;
                                    }
                                });

    /*
    BukkitUtils.setEntitySpeed(entity, input);
    Default speed for horse:
                                        return (0.44999998807907104D +
                                            this.random.nextDouble() * 0.3D +
                                            this.random.nextDouble() * 0.3D
                                            + this.random.nextDouble() * 0.3D)
                                            * 0.25D;
     */

    public static final EntityDataChanger<Living> ENTITY_NAME =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, String>() {
                                    @Override
                                    public void applyEntity(Living entity, String input)
                                    {
                                        entity.offer(Keys.DISPLAY_NAME, TextSerializers.FORMATTING_CODE.deserialize(input));
                                    }

                                    @Override
                                    public String getTypeValue(String input)
                                    {
                                        if (input.startsWith("name_"))
                                        {
                                            return input.substring(5, input.length());
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Entity> TAMEABLE =
        new EntityDataChanger<>(Entity.class,
                        new BoolEntityChanger<Entity>("tamed") {
                            @Override
                            public void applyEntity(Entity entity, Boolean value) {
                                if (entity.supports(Keys.TAMED_OWNER))
                                {
                                    entity.offer(Keys.TAMED_OWNER, java.util.Optional.empty());
                                }
                            }
                        });

    public static final EntityDataChanger<Entity> TAMER =
        new EntityDataChanger<>(Entity.class,
                                new EntityChanger<Entity, User>() {
                                    @Override
                                    public void applyEntity(Entity entity, User value) {
                                        if (entity.supports(Keys.TAMED_OWNER))
                                        {
                                            entity.offer(Keys.TAMED_OWNER, java.util.Optional.of(value.getUniqueId()));
                                        }
                                    }

                                    @Override
                                    public User getTypeValue(String input)
                                    {
                                        if (input.startsWith("tamer_"))
                                        {
                                            String userName = input.substring(6, input.length());
                                            return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(userName).orElse(null);
                                        }
                                        return null;
                                    }
                                });

    /* TODO eventually #tm)
    public static final EntityDataChanger<Living> ARMOR_CHESTPLATE =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, ItemStack>() {
                                    @Override
                                    public void applyEntity(Living entity, ItemStack value) {
                                        entity.getEquipment().setChestplate(value);
                                    }

                                    @Override
                                    public ItemStack getTypeValue(String input)
                                    {
                                        if (input.startsWith("armor_") || input.startsWith("chest_"))
                                        {
                                            return Match.material().itemStack(input.substring(6, input.length()));
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Living> ARMOR_LEG =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, ItemStack>() {
                                    @Override
                                    public void applyEntity(Living entity, ItemStack value) {
                                        entity.getEquipment().setLeggings(value);
                                    }

                                    @Override
                                    public ItemStack getTypeValue(String input)
                                    {
                                        if (input.startsWith("leg_"))
                                        {
                                            return Match.material().itemStack(input.substring(4, input.length()));
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Living> ARMOR_BOOT =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, ItemStack>() {
                                    @Override
                                    public void applyEntity(Living entity, ItemStack value) {
                                        entity.getEquipment().setBoots(value);
                                    }

                                    @Override
                                    public ItemStack getTypeValue(String input)
                                    {
                                        if (input.startsWith("boot_"))
                                        {
                                            return Match.material().itemStack(input.substring(5, input.length()));
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Living> ARMOR_HELMET =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, ItemStack>() {
                                    @Override
                                    public void applyEntity(Living entity, ItemStack value) {
                                        entity.getEquipment().setHelmet(value);
                                    }

                                    @Override
                                    public ItemStack getTypeValue(String input)
                                    {
                                        if (input.startsWith("helmet_"))
                                        {
                                            return Match.material().itemStack(input.substring(7, input.length()));
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Living> EQUIP_HAND =
        new EntityDataChanger<>(Living.class,
                                new EntityChanger<Living, ItemStack>() {
                                    @Override
                                    public void applyEntity(Living entity, ItemStack value) {
                                        entity.getEquipment().setItemInHand(value);
                                    }

                                    @Override
                                    public ItemStack getTypeValue(String input)
                                    {
                                        if (input.startsWith("inhand_"))
                                        {
                                            return Match.material().itemStack(input.substring(7, input.length()));
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Living> DO_DROP_EQUIP =
        new EntityDataChanger<>(Living.class,
                                new BoolEntityChanger<Living>("dropEquip") {
                                    @Override
                                    public void applyEntity(Living entity, Boolean value) {
                                        if (value)
                                        {
                                            EntityEquipment equipment = entity.getEquipment();
                                            equipment.setBootsDropChance(1.1F);
                                            equipment.setLeggingsDropChance(1F);
                                            equipment.setChestplateDropChance(1.1F);
                                            equipment.setHelmetDropChance(1F);
                                            equipment.setItemInHandDropChance(1F);
                                        }
                                    }
                                });

    public static final EntityDataChanger<Living> DO_NOT_DROP_EQUIP =
        new EntityDataChanger<>(Living.class,
                                new BoolEntityChanger<Living>("dropNoEquip") {
                                    @Override
                                    public void applyEntity(Living entity, Boolean value) {
                                        if (value)
                                        {
                                            EntityEquipment equipment = entity.getEquipment();
                                            equipment.setBootsDropChance(0F);
                                            equipment.setLeggingsDropChance(0F);
                                            equipment.setChestplateDropChance(0F);
                                            equipment.setHelmetDropChance(0F);
                                            equipment.setItemInHandDropChance(0F);
                                        }
                                    }
                                });
                */

    public static final EntityDataChanger<Horse> HORSE_COLOR =
        new EntityDataChanger<>(Horse.class,
                                new MappedEntityChanger<Horse, HorseColor>() {
                                    @Override
                                    void fillValues()
                                    {
                                        this.map.put("white", HorseColors.WHITE);
                                        this.map.put("creamy", HorseColors.CREAMY);
                                        this.map.put("chestnut", HorseColors.CHESTNUT);
                                        this.map.put("brown", HorseColors.BROWN);
                                        this.map.put("black", HorseColors.BLACK);
                                        this.map.put("gray", HorseColors.GRAY);
                                        this.map.put("darkbrown", HorseColors.DARK_BROWN);
                                    }

                                    @Override
                                    public void applyEntity(Horse entity, HorseColor input)
                                    {
                                        entity.offer(Keys.HORSE_COLOR, input);
                                    }
                                });

    /*
    public static final EntityDataChanger<Horse> HORSE_VARIANT =
        new EntityDataChanger<>(Horse.class,
                                new MappedEntityChanger<Horse, HorseVariant>() {
                                    @Override
                                    void fillValues()
                                    {
                                        this.map.put("horse", HorseVariants.HORSE);
                                        this.map.put("donkey", HorseVariants.DONKEY);
                                        this.map.put("mule", HorseVariants.MULE);
                                        this.map.put("undead", HorseVariants.UNDEAD_HORSE);
                                        this.map.put("skeleton", HorseVariants.SKELETON_HORSE);
                                    }

                                    @Override
                                    public void applyEntity(Horse entity, HorseVariant input)
                                    {
                                        entity.offer(Keys.HORSE_VARIANT, input);
                                    }
                                });

    public static final EntityDataChanger<Horse> HORSE_STYLE =
        new EntityDataChanger<>(Horse.class,
                                new MappedEntityChanger<Horse, HorseStyle>() {
                                    @Override
                                    void fillValues()
                                    {
                                        this.map.put("stylenone", HorseStyles.NONE);
                                        this.map.put("stylewhite", HorseStyles.WHITE);
                                        this.map.put("whitefield", HorseStyles.WHITEFIELD);
                                        this.map.put("whitedots", HorseStyles.WHITE_DOTS);
                                        this.map.put("blackdots", HorseStyles.BLACK_DOTS);
                                    }

                                    @Override
                                    public void applyEntity(Horse entity, HorseStyle input)
                                    {
                                        entity.offer(Keys.HORSE_STYLE, input);
                                    }
                                });
    */

    private EntityDataChanger(Class<EntityInterface> clazz, EntityChanger<EntityInterface, ?> changer)
    {
        this.clazz = clazz;
        this.changer = changer;
        entityDataChangers.add(this);
    }

    @SuppressWarnings("unchecked")
    public boolean applyTo(Entity entity, String value)
    {
        if (canApply(entity))
        {
            this.changer.applyEntity((EntityInterface)entity, value);
            return true;
        }
        return false;
    }

    public boolean canApply(Entity entity)
    {
        return clazz.isAssignableFrom(entity.getClass());
    }

    public static abstract class EntityChanger<E,T>
    {
        public void applyEntity(E entity, String input)
        {
            T typeValue = this.getTypeValue(input);
            if (typeValue != null) this.applyEntity(entity, typeValue);
        }
        public abstract void applyEntity(E entity, T convertedInput);
        public abstract T getTypeValue(String input);
    }

    private static abstract class BoolEntityChanger<E> extends EntityChanger<E, Boolean>
    {
        private final List<String> names;
        protected BoolEntityChanger(String... names)
        {
            this.names = Arrays.asList(names);
        }

        @Override
        public Boolean getTypeValue(String input)
        {
            for (String name : names)
            {
                if (name.equalsIgnoreCase(input))
                {
                    return true;
                }
            }
            return null;
        }
    }

    private static abstract class MappedEntityChanger<E, T> extends EntityChanger<E, T>
    {
        protected final Map<String, T> map = new HashMap<>();

        protected MappedEntityChanger()
        {
            this.fillValues();
        }

        @Override
        public T getTypeValue(String input)
        {
            return map.get(input);
        }

        abstract void fillValues();
    }
}
