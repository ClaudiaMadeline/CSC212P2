package edu.smith.cs.csc212.p2;

public class FallingRock extends Rock
{
	// FallingRock is a special kind of Rock - it can move down!
	
	/**
	 * Construct a FallingRock in our world.
	 * @param world - the grid world.
	 * @param rock - the rock itself
	 */
	public FallingRock(World world, Rock rock) 
	{
		super(world);
	}
	
	// allows the FallingRock to fall
	@Override
	public void step() 
	{
		this.moveDown();		
	}
}
