import javafx.scene.control.Button;
import ship.*;

/**
 * SeaBattleButton
 */
public class SeaBattleButton extends Button {

    private CellType cellType = CellType.EMPTY;
    private Ship ship = null;
    private Cell cell;

    public SeaBattleButton(CellType cellType, int col, int row)
    {   
        super();
        this.cellType = cellType;
        this.cell = new Cell(cellType.getCode(), col, row);
        setShip(cellType);

    }

   
    public enum CellType 
    {
        EMPTY(ASBTP.EMPTY, null),
        SOLDIER(ASBTP.SOLDIER, new Soldier()),
        CAVALRY(ASBTP.CAVALRY, new Cavalry()),
        FORT(ASBTP.FORT, new Fort()),
        HEADQUARTERS(ASBTP.HEADQUARTERS, new HeadQuarters());

        private int code;
        private Ship ship;

        CellType(int code, Ship ship)
        {
            this.code = code;
            this.ship = ship;
        }
        /**
         * @return the code
         */
        public int getCode() {
            return code;
        }
        /**
         * @return the ship
         */
        public Ship getShip() {
            return ship;
        }
        

    }

    
    /**
     * @param cell the cell to set
     */
    public void setCell(Cell cell) {
        this.cell = cell;
    }
    /**
     * @return the cell
     */
    public Cell getCell() {
        return cell;
    }

  
    /**
     * @return the ship
     */
    public Ship getShip() {
        return ship;
    }
    /**
     * @return the childShips
     */
   
    public void setShip(CellType cellType) {
        switch(cellType)
        {
            case SOLDIER:
                ship = new Soldier();
                this.cellType = CellType.SOLDIER;
                this.cell.setCellType(CellType.SOLDIER.getCode());

                break;
            case CAVALRY:
                ship = new Cavalry();
                this.cellType = CellType.CAVALRY;
                this.cell.setCellType(CellType.CAVALRY.getCode());


                break;
            case FORT:
                ship = new Fort();
                this.cellType = CellType.FORT;
                this.cell.setCellType(CellType.FORT.getCode());


                break;
            case HEADQUARTERS:
                ship = new HeadQuarters();
                this.cellType = CellType.HEADQUARTERS;
                this.cell.setCellType(CellType.HEADQUARTERS.getCode());


                break;
            default:
                
                ship = null;
                this.cell.setCellType(CellType.EMPTY.getCode());

                this.cellType = CellType.EMPTY;
        }
    }
    
    /**
     * @return the cellType
     */
    public CellType getCellType() {
        return cellType;
    }
    /**
     * @param cellType the cellType to set
     */

}