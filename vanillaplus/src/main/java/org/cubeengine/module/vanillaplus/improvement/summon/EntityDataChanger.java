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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.CatType;
import org.spongepowered.api.data.type.CatTypes;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.ProfessionType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Ageable;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Cat;
import org.spongepowered.api.entity.living.animal.Ocelot;
import org.spongepowered.api.entity.living.animal.Pig;
import org.spongepowered.api.entity.living.animal.Sheep;
import org.spongepowered.api.entity.living.animal.Wolf;
import org.spongepowered.api.entity.living.animal.horse.Horse;
import org.spongepowered.api.entity.living.animal.horse.TameableHorse;
import org.spongepowered.api.entity.living.monster.Creeper;
import org.spongepowered.api.entity.living.monster.Enderman;
import org.spongepowered.api.entity.living.monster.slime.Slime;
import org.spongepowered.api.entity.living.monster.zombie.ZombifiedPiglin;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.trader.Villager;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;


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
                        entity.offer(Keys.IS_SADDLED, input);
                    }
                });

    public static final EntityDataChanger<TameableHorse> HORSE_SADDLE =
        new EntityDataChanger<>(TameableHorse.class,
                                new BoolEntityChanger<TameableHorse>("saddled")
                                {
                                    @Override
                                    public void applyEntity(TameableHorse entity, Boolean input)
                                    {
                                        entity.offer(Keys.IS_SADDLED, input);
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

    public static final EntityDataChanger<Wolf> WOLF_ANGRY =
            new EntityDataChanger<>(Wolf.class,
                    new BoolEntityChanger<Wolf>("angry") {
                        @Override
                        public void applyEntity(Wolf entity, Boolean input) {
                            entity.offer(Keys.IS_ANGRY, input);
                        }
                    });

    public static final EntityDataChanger<ZombifiedPiglin> PIGZOMBIE_ANGRY =
        new EntityDataChanger<>(ZombifiedPiglin.class,
                          new BoolEntityChanger<ZombifiedPiglin>("angry") {
                              @Override
                              public void applyEntity(ZombifiedPiglin entity, Boolean input) {
                                  entity.offer(Keys.IS_ANGRY, input);
                              }
                          });

    public static final EntityDataChanger<Creeper> CREEPER_POWERED =
            new EntityDataChanger<>(Creeper.class,
                    new BoolEntityChanger<Creeper>("powered", "power", "charged") {
                        @Override
                        public void applyEntity(Creeper entity, Boolean input) {
                            entity.offer(Keys.IS_CHARGED, input);
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


    public static final EntityDataChanger<Cat> CAT_TYPE =
        new EntityDataChanger<>(Cat.class,
            new MappedEntityChanger<Cat, CatType>() {
                @Override
                void fillValues()
                {
                    this.map.put("all_black", CatTypes.ALL_BLACK.get());
                    this.map.put("black", CatTypes.BLACK.get());
                    this.map.put("british_shorthair", CatTypes.BRITISH_SHORTHAIR.get());
                    this.map.put("calico", CatTypes.CALICO.get());
                    this.map.put("jellie", CatTypes.JELLIE.get());
                    this.map.put("persian", CatTypes.PERSIAN.get());
                    this.map.put("ragdoll", CatTypes.RAGDOLL.get());
                    this.map.put("red", CatTypes.RED.get());
                    this.map.put("siamese", CatTypes.SIAMESE.get());
                    this.map.put("tabby", CatTypes.TABBY.get());
                    this.map.put("white", CatTypes.WHITE.get());
                }

                @Override
                public void applyEntity(Cat entity, CatType input)
                {
                    entity.offer(Keys.CAT_TYPE, input);
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
                            return findRegistryValue(input, RegistryTypes.DYE_COLOR).orElse(null);
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
                             final List<DyeColor> list = Sponge.game().registries().registry(RegistryTypes.DYE_COLOR).stream().collect(Collectors.toList());
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
                             return findRegistryValue(input, RegistryTypes.DYE_COLOR).orElse(null);
                         }
                     });

    public static  final EntityDataChanger<Villager> VILLAGER_PROFESSION =
            new EntityDataChanger<>(Villager.class,
                    new EntityChanger<Villager, ProfessionType>() {
                        @Override
                        public void applyEntity(Villager entity, ProfessionType input)
                        {
                            entity.offer(Keys.PROFESSION_TYPE, input);
                        }

                        @Override
                        public ProfessionType getTypeValue(String input)
                        {
                            return findRegistryValue(input, RegistryTypes.PROFESSION_TYPE).orElse(null);
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
                            return findRegistryValue(input, RegistryTypes.ITEM_TYPE).map(ItemStack::of).orElse(null);
                        }
                    });

    private static <T> Optional<T> findRegistryValue(String input, RegistryType<T> itemType)
    {
        return Sponge.game().registries().registry(itemType).findValue(ResourceKey.resolve(input));
    }

    public static final EntityDataChanger<Slime> SLIME_SIZE =
        new EntityDataChanger<>(Slime.class,
             new EntityChanger<Slime,Integer>() {
                 @Override
                 public void applyEntity(Slime entity, Integer input)
                 {
                     entity.offer(Keys.SIZE, input);
                 }

                 private StringMatcher sm = new StringMatcher(); // TODO this is suboptimal (I agree - pschichtel)

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
                                        entity.offer(Keys.CUSTOM_NAME, LegacyComponentSerializer.legacyAmpersand().deserialize(input));
                                    }

                                    @Override
                                    public String getTypeValue(String input)
                                    {
                                        if (input.startsWith("name_"))
                                        {
                                            return input.substring(5);
                                        }
                                        return null;
                                    }
                                });

    public static final EntityDataChanger<Entity> TAMEABLE =
        new EntityDataChanger<>(Entity.class,
                        new BoolEntityChanger<Entity>("tamed") {
                            @Override
                            public void applyEntity(Entity entity, Boolean value) {
                                if (entity.supports(Keys.IS_TAMED))
                                {
                                    entity.offer(Keys.IS_TAMED, true);
                                }
                            }
                        });

    public static final EntityDataChanger<Entity> TAMER =
        new EntityDataChanger<>(Entity.class,
                                new EntityChanger<Entity, User>() {
                                    @Override
                                    public void applyEntity(Entity entity, User value) {
                                        if (entity.supports(Keys.TAMER))
                                        {
                                            entity.offer(Keys.TAMER, value.uniqueId());
                                        }
                                    }

                                    @Override
                                    public User getTypeValue(String input)
                                    {
                                        if (input.startsWith("tamer_"))
                                        {
                                            String userName = input.substring(6);
                                            return Sponge.server().userManager().find(userName).orElse(null);
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
                                        this.map.put("white", HorseColors.WHITE.get());
                                        this.map.put("creamy", HorseColors.CREAMY.get());
                                        this.map.put("chestnut", HorseColors.CHESTNUT.get());
                                        this.map.put("brown", HorseColors.BROWN.get());
                                        this.map.put("black", HorseColors.BLACK.get());
                                        this.map.put("gray", HorseColors.GRAY.get());
                                        this.map.put("darkbrown", HorseColors.DARK_BROWN.get());
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
