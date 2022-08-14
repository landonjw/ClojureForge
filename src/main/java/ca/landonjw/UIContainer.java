package ca.landonjw;

import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;

import javax.annotation.Nullable;

// Hack to avoid needing to do reflection with container.
public abstract class UIContainer extends Container
{
    protected UIContainer(@Nullable ContainerType<?> containerType)
    {
        super(containerType, 1);
    }

    public void addNewSlot(Slot slot)
    {
        addSlot(slot);
    }
}
