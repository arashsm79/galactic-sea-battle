/*

    Developed by @ArashSM79

*/
import java.util.ArrayList;

public class Cell {


    private int cellType = 1;
    private ArrayList<Point> childShips = null;
    private Point headShipCordinates = null;
    private int row;
    private int col;
    private int coolDown = 0;
    private boolean isDestroyed = false;


    Cell(int cellType, int col, int row)
    {
        this.cellType = cellType;
        this.row = row;
        this.col = col;
        childShips = new ArrayList<Point>();
    }

    
    /**
     * @return the coldDown
     */
    public int getCoodDown() {
        return coolDown;
    }
    /**
     * @param coldDown the coldDown to set
     */
    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    /**
     * @return the cellType
     */
    public int getCellType() {
        return cellType;
    }/**
     * @return the col
     */
    public int getCol() {
        return col;
    }/**
     * @return the headShipCordinates
     */
    public Point getHeadShipCordinates() {
        return headShipCordinates;
    }/**
     * @return the row
     */
    public int getRow() {
        return row;
    }
    /**
     * @param cellType the cellType to set
     */
    public void setCellType(int cellType) {
        this.cellType = cellType;
    }/**
     * @param childShips the childShips to set
     */
    public void setChildShips(ArrayList<Point> childShips) {
        this.childShips = childShips;
    }/**
     * @param col the col to set
     */
    public void setCol(int col) {
        this.col = col;
    }/**
     * @param isDestroyed the isDestroyed to set
     */
    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }/**
     * @param headShipCordinates the headShipCordinates to set
     */
    public void setHeadShipCordinates(Point headShipCordinates) {
        this.headShipCordinates = headShipCordinates;
    }/**
     * @param row the row to set
     */
    public void setRow(int row) {
        this.row = row;
    }
    /**
     * @return the isDestroyed
     */
    public boolean isDestroyed() {
        return isDestroyed;
    }
    /**
     * @return the childShips
     */
    public ArrayList<Point> getChildShips() {
        return childShips;
    }


	public void resetChildShips() {
        this.childShips = new ArrayList<Point>();
	}
    
    
}
