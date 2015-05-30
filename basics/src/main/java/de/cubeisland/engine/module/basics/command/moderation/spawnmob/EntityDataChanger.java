/**
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
package de.cubeisland.engine.module.basics.command.moderation.spawnmob;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.core.sponge.BukkitUtils;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.service.user.User;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.DyeableData;
import org.spongepowered.api.data.manipulator.entity.ChargedData;
import org.spongepowered.api.data.manipulator.entity.HealthData;
import org.spongepowered.api.data.manipulator.entity.SaddleData;
import org.spongepowered.api.data.manipulator.entity.ShearedData;
import org.spongepowered.api.data.manipulator.entity.SittingData;
import org.spongepowered.api.data.manipulator.entity.VillagerZombieData;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.HorseStyle;
import org.spongepowered.api.data.type.HorseStyles;
import org.spongepowered.api.data.type.HorseVariant;
import org.spongepowered.api.data.type.HorseVariants;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.data.type.OcelotTypes;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.data.type.SkeletonTypes;
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
import org.spongepowered.api.entity.living.monster.Skeleton;
import org.spongepowered.api.entity.living.monster.Slime;
import org.spongepowered.api.entity.living.monster.Zombie;
import org.spongepowered.api.entity.living.monster.ZombiePigman;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;

import static org.spongepowered.api.item.ItemTypes.SADDLE;
import static org.spongepowered.api.text.Texts.fromLegacy;


public class EntityDataChanger<EntityInterface>
{
    private final Class<EntityInterface> clazz;
    protected final EntityChanger<EntityInterface, ?> changer;
    public static final Set<EntityDataChanger> entityDataChangers = new HashSet<>();

    static Game game; // TODO

    public static final EntityDataChanger<Pig> PIG_SADDLE =
            new EntityDataChanger<>(Pig.class,
                new BoolEntityChanger<Pig>("saddled")
                {
                    @Override
                    public void applyEntity(Pig entity, Boolean input)
                    {
                        entity.offer(entity.getOrCreate(SaddleData.class).get().setSaddle(game.getRegistry().getItemBuilder().itemType(SADDLE).build()));
                    }
                });

    public static final EntityDataChanger<Horse> HORSE_SADDLE =
        new EntityDataChanger<>(Horse.class,
                                new BoolEntityChanger<Horse>("saddled")
                                {
                                    @Override
                                    public void applyEntity(Horse entity, Boolean input)
                                    {
                                        Optional<SaddleData> saddleData = entity.getOrCreate(SaddleData.class);
                                        if (saddleData.isPresent())
                                        {
                                            entity.offer(saddleData.get().setSaddle(
                                                game.getRegistry().getItemBuilder().itemType(SADDLE).build()));
                                        }
                                    }
                                });

    public static final EntityDataChanger<Ageable> AGEABLE_BABY =
            new EntityDataChanger<>(Ageable.class,
                    new BoolEntityChanger<Ageable>("baby") {
                        @Override
                        public void applyEntity(Ageable entity, Boolean input)
                        {
                            if (input)
                            {
                                entity.offer(entity.getAgeData().setBaby());
                            }
                            else
                            {
                                entity.offer(entity.getAgeData().setAdult());
                            }
                        }
                    });

    public static final EntityDataChanger<Zombie> ZOMBIE_VILLAGER =
        new EntityDataChanger<>(Zombie.class,
          new BoolEntityChanger<Zombie>("villager") {
              @Override
              public void applyEntity(Zombie entity, Boolean input) {
                  if (input)
                  {
                      entity.offer(entity.getOrCreate(VillagerZombieData.class).get());
                  }
                  else
                  {
                      entity.remove(VillagerZombieData.class);
                  }
              }
          });

    public static final EntityDataChanger<Wolf> WOLF_ANGRY =
            new EntityDataChanger<>(Wolf.class,
                    new BoolEntityChanger<Wolf>("angry") {
                        @Override
                        public void applyEntity(Wolf entity, Boolean input) {
                            entity.setAngry(input);
                        }
                    });

    public static final EntityDataChanger<ZombiePigman> PIGZOMBIE_ANGRY =
        new EntityDataChanger<>(ZombiePigman.class,
                          new BoolEntityChanger<ZombiePigman>("angry") {
                              @Override
                              public void applyEntity(ZombiePigman entity, Boolean input) {
                                  entity.setAngry(input);
                              }
                          });

    public static final EntityDataChanger<Creeper> CREEPER_POWERED =
            new EntityDataChanger<>(Creeper.class,
                    new BoolEntityChanger<Creeper>("powered", "power", "charged") {
                        @Override
                        public void applyEntity(Creeper entity, Boolean input) {
                            if (input)
                            {
                                entity.offer(entity.getOrCreate(ChargedData.class).get());
                            }
                            else
                            {
                                entity.remove(ChargedData.class);
                            }
                        }
                    });

    public static final EntityDataChanger<Wolf> WOLF_SIT =
            new EntityDataChanger<>(Wolf.class,
                    new BoolEntityChanger<Wolf>("sitting", "sit") {
                        @Override
                        public void applyEntity(Wolf entity, Boolean input) {
                            if (input)
                            {
                                entity.offer(entity.getOrCreate(SittingData.class).get());
                            }
                            else
                            {
                                entity.remove(SittingData.class);
                            }
                        }
                    });

    public static final EntityDataChanger<Ocelot> OCELOT_SIT =
        new EntityDataChanger<>(Ocelot.class,
         new BoolEntityChanger<Ocelot>("sitting", "sit") {
             @Override
             public void applyEntity(Ocelot entity, Boolean input) {
                 if (input)
                 {
                     entity.offer(entity.getOrCreate(SittingData.class).get());
                 }
                 else
                 {
                     entity.remove(SittingData.class);
                 }
             }
         });

    public static final EntityDataChanger<Skeleton> SKELETON_TYPE =
        new EntityDataChanger<>(Skeleton.class,
         new BoolEntityChanger<Skeleton>("wither") {
             @Override
             public void applyEntity(Skeleton entity, Boolean input)
             {
                 entity.offer(entity.getSkeletonData().setValue(input ? SkeletonTypes.WITHER : SkeletonTypes.NORMAL));
             }
         });

    public static final EntityDataChanger<Sheep> SHEEP_SHEARED =
        new EntityDataChanger<>(Sheep.class,
                     new BoolEntityChanger<Sheep>("sheared") {
                         @Override
                         public void applyEntity(Sheep entity, Boolean input)
                         {
                             if (input)
                             {
                                 entity.offer(entity.getOrCreate(ShearedData.class).get());
                             }
                             else
                             {
                                 entity.remove(ShearedData.class);
                             }
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
                      entity.offer(entity.getOcelotData().setValue(input));
                  }
              });

    public static  final EntityDataChanger<Sheep> SHEEP_COLOR =
            new EntityDataChanger<>(Sheep.class,
                    new EntityChanger<Sheep, DyeColor>() {
                        @Override
                        public void applyEntity(Sheep entity, DyeColor input)
                        {
                            entity.offer(entity.getDyeData().setValue(input));
                        }
                        @Override
                        public DyeColor getTypeValue(String input)
                        {
                            return Match.materialData().colorData(input);
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
                             entity.offer(entity.getDyeData().setValue(randomColor));
                         }
                     }
                 });


    public static final EntityDataChanger<Wolf> WOLF_COLLAR =
        new EntityDataChanger<>(Wolf.class,
                   new EntityChanger<Wolf, DyeColor>() {
                         @Override
                         public void applyEntity(Wolf entity, DyeColor input) {
                             entity.offer(entity.getOrCreate(DyeableData.class).get().setValue(input));
                         }
                         @Override
                         public DyeColor getTypeValue(String input)
                         {
                             return Match.materialData().colorData(input);
                         }
                     });

    public static  final EntityDataChanger<Villager> VILLAGER_PROFESSION =
            new EntityDataChanger<>(Villager.class,
                    new EntityChanger<Villager, Career>() {
                        @Override
                        public void applyEntity(Villager entity, Career input)
                        {
                            entity.offer(entity.getCareerData().setCareer(input));
                        }

                        @Override
                        public Career getTypeValue(String input)
                        {
                            return Match.profession().profession(input);
                        }
                    });

    public static final EntityDataChanger<Enderman> ENDERMAN_ITEM =
            new EntityDataChanger<>(Enderman.class,
                    new EntityChanger<Enderman, ItemStack>() {
                        @Override
                        public void applyEntity(Enderman entity, ItemStack value)
                        {
                            if (value.getType().isBlock())
                            {
                                entity.setCarriedMaterial(value.getData());
                            }
                        }

                        @Override
                        public ItemStack getTypeValue(String input)
                        {
                            return Match.material().itemStack(input);
                        }
                    });

    public static final EntityDataChanger<Slime> SLIME_SIZE =
        new EntityDataChanger<>(Slime.class,
                                             new EntityChanger<Slime,Integer>() {
                                                 @Override
                                                 public void applyEntity(Slime entity, Integer input)
                                                 {
                                                     entity.offer(entity.getSlimeData().setSize(input));
                                                 }

                                                 @Override
                                                 public Integer getTypeValue(String input)
                                                 {
                                                     String match = Match.string().matchString(input, "tiny", "small", "big");
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
                            HealthData healthData = entity.getHealthData();
                            healthData.setMaxHealth(input);
                            healthData.setHealth(input);
                            entity.offer(healthData);
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
                                        entity.setJumpStrength(input);
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
                                        BukkitUtils.setEntitySpeed(entity, input);
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
                                        entity.getOrCreate(DisplayNameData.class).get().setDisplayName(fromLegacy(input, '&'));
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

    public static final EntityDataChanger<Tameable> TAMEABLE =
        new EntityDataChanger<>(Tameable.class,
                        new BoolEntityChanger<Tameable>("tamed") {
                            @Override
                            public void applyEntity(Tameable entity, Boolean value) {
                                entity.setTamed(value);
                            }
                        });

    public static final EntityDataChanger<Tameable> TAMER =
        new EntityDataChanger<>(Tameable.class,
                                new EntityChanger<Tameable, AnimalTamer>() {
                                    @Override
                                    public void applyEntity(Tameable entity, AnimalTamer value) {
                                        entity.setOwner(value);
                                    }

                                    @Override
                                    public AnimalTamer getTypeValue(String input)
                                    {
                                        if (input.startsWith("tamer_"))
                                        {
                                            String userName = input.substring(6, input.length());
                                            User user = CubeEngine.getUserManager().findUser(userName);
                                            return user;
                                        }
                                        return null;
                                    }
                                });

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

    public static final EntityDataChanger<Horse> HORSE_COLOR =
        new EntityDataChanger<>(Horse.class,
                                new MappedEntityChanger<Horse, HorseColor>() {
                                    @Override
                                    void fillValues()
                                    {
                                        HorseColors
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
                                        entity.offer(entity.getHorseData().setColor(input));
                                    }
                                });

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
                                        entity.offer(entity.getHorseData().setVariant(input));
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
                                        entity.offer(entity.getHorseData().setStyle(input));
                                    }
                                });


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
