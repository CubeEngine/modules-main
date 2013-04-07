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
package de.cubeisland.cubeengine.core.util;

/**
 * Represents a bitmask with 32 bits
 */
public class BitMask
{
    private int mask;

    /**
     * Creates a new BitMask
     *
     */
    public BitMask()
    {
        this(0);
    }

    /**
     * Creates a new BitMask and set its value to mask
     *
     * @param mask the value to set the BitMask at the beginning
     */
    public BitMask(int mask)
    {
        this.mask = mask;
    }

    /**
     * Gets the value of this BitMask
     *
     * @return the value saved by this BitMask
     */
    public int get()
    {
        return this.mask;
    }

    /**
     * Sets the specified Bits
     *
     * @param bits The bits to set
     * @return the new value of this BitMask
     */
    public int set(int bits)
    {
        return this.mask |= bits;
    }

    /**
     * Resets this BitMask to 0
     *
     * @return 0
     */
    public int reset()
    {
        return this.reset(0);
    }

    /**
     * Resets this BitMask to mask
     *
     * @param mask The value to reset the BitMask to
     * @return the new value of this BitMask
     */
    public int reset(int mask)
    {
        return this.mask = mask;
    }

    /**
     * Unsets the specified Bits
     *
     * @param bits The bits to unset
     * @return the new value of this BitMask
     */
    public int unset(int bits)
    {
        return this.mask &= ~bits;
    }

    /**
     * Toggles the specified Bits
     *
     * @param bits The bits to toggle
     * @return the new value of this BitMask
     */
    public int toggle(int bits)
    {
        return this.mask ^= bits;
    }

    /**
     * Checks whether the specified bits are set.
     *
     * @param bits The bits to check.
     * @return whether the specified bits are set or not
     */
    public boolean isset(int bits)
    {
        return ((this.mask & bits) == bits);
    }
}
