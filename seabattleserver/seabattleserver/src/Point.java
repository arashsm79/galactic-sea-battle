
/*

    Developed by @ArashSM79

*/
public class Point {

    //================================================================

    private int row;
    private int col;

    //================================================================

    public Point(int col, int row)
    {
        this.col = col;
        this.row = row;
    }

    //================================================================

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Point)
        {
            Point p = (Point) obj;
            
            if(p.getCol() == this.col
            && p.getRow() == this.row)
            {
                return true;
            }
        }
        return super.equals(obj);
    }
    /**
     * @param row the row to set
     */
    public void setRow(int row) {
        this.row = row;
    }
    /**
     * @param col the col to set
     */
    public void setCol(int col) {
        this.col = col;
    }
    /**
     * @return the col
     */
    public int getCol() {
        return col;
    }
    /**
     * @return the row
     */
    public int getRow() {
        return row;
    }
}