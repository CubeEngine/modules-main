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
package de.cubeisland.engine.log.action.newaction.block.ignite;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.log.action.newaction.ActionTypeBase;
import de.cubeisland.engine.log.action.newaction.block.BlockActionType;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

public class LightningIgnite extends BlockActionType<BlockIgniteListener>
{
    // return "lightning-ignite";
    // return this.lm.getConfig(world).block.ignite.LIGHTNING_IGNITE_enable;


    @Override
    public boolean canAttach(ActionTypeBase action)
    {
        return action instanceof LightningIgnite;
    }

    @Override
    public String translateAction(User user)
    {
        int count = this.countAttached();
        return user
            .getTranslationN(POSITIVE, count, "A fire got set by a lightning strike", "{amount} fires got set by lightning strikes", count);
    }
}
