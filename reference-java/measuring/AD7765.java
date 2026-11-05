package measuring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import javax.swing.JOptionPane;

import program.Communication;
import program.Main;
import program.ProgStates;
import program.Property;
import tools.MyMath;

/*
 			--------------------------------------------------------------------------------------
	Frame:  | Preamble | Status Byte | Messwerte (Spannung) | Messwerte (Strom) | Counter | CRC |
			--------------------------------------------------------------------------------------
			
			Preamble		:	1 Byte
			Statusbyte		:	1 Byte		(State3|State2|State1|State0|0|0|0|0)
			Messwerte (U)	:	8 x 3 Byte	(Bit 3 - 0 der Messwerte -> Indikator)
			Messwerte (I)	:	8 x 3 Byte	(Bit 3 - 0 der Messwerte -> Indikator)
			Counter			:	2 Byte		(Check, ob alle Daten ankommen
			CRC				:	2 Byte
			
			=> 54 Byte
 */

public class AD7765 extends Device{
	public final static int VALUE_MASK = 0xFFFFFFF0;
	public final static int RANGE_MASK = 0x07;
	
	private int[] values = new int[2];		//{rangeI, valueI, valueU}
	private long time_startMessung;
	
	private byte[] frame;
	private int crc;
	private int lastI;
	private int lastU;
	
	private final int timeoutTreashold = 2000;			//warte 5 Sekunden auf Startsequenz
	private final byte POS_I = 5;						//Startposition der Stromwerte
	private final byte POS_U = 2;						//Startposition der Spannungswerte
	private final byte POS_STATE = 1;					//Position des StateBytes
	private final int FRAME_SIZE = 8;					//Anzahl der Messwerte pro Frame
	private final int FRAME_FULL_SIZE = 54;				//Anzahl der Bytes pro Frame
	private final byte STARTFRAME = (byte) 0xe5;
	
	private int os = Property.getPropertiesInt(Property.MESSUNG_OVERSAMPLING);
	private boolean os_enable = Property.getPropertiesBoolean(Property.MESSUNG_OVERSAMPLING_ENABLE);
	private int os_counter = os;

	public AD7765(Main main) {
		super(main);
    	anzMesswerte = 0;
    	time_startMessung = 0;
    	frame = new byte[FRAME_FULL_SIZE - 2];
    	crc = 0;
    	lastI = 0;
    	lastU = 0;
	}
	
	@Override
	public void startMe(){
    	Messzeit = (new Date()).getTime();
        stateList.add(new State(State.S_START,time_startMessung));
        long frameCounter = 0;
        int crcErrors = 0;
        int counter = -1;
        int preonCounter = 0;
//        int missingFrames = 0;
        int communicationError = 0;
        String log = "";
        
		try{
			while (ProgStates.MEASSURE_RUNS){			
				byte[] temp_frame = Communication.um232H_getValidFrame(FRAME_FULL_SIZE, STARTFRAME, timeoutTreashold);
				if (temp_frame == null){
					if ((ProgStates.ISCONNECTED || ProgStates.useFT232HOnly) && ProgStates.ISCONNECTED_FAST) System.out.println("timeout");
					_main.gui.myMenue.cmd_mnuStopMessung.doClick();
					break;
				}
								
				System.arraycopy(temp_frame, 0, frame, 0, FRAME_FULL_SIZE - 2);						//Kopiere die restlichen Daten ohne CRC in den Frame 
				crc = (ByteBuffer.wrap(temp_frame, FRAME_FULL_SIZE - 2, 2).getShort()) & 0xFFFF;		//lese CRC-Wert
				
				int crc_check = MyMath.crc16_data(frame, 0);	

				if (crc_check != crc ){									//Wenn CRC-Check failed nehme die letzten Messwerte 
					values[0] = lastI;					
					values[1] = lastU;
					crcErrors++;
					for (int i=0;i<FRAME_SIZE;i++){
						_main.data.addValueToMessdaten(0, values[0],0,false);
						_main.data.addValueToMessdaten(1, values[1],0,false);		
						anzMesswerte++;
					}
					counter++;
					System.out.println("CRC-Fehler an Pos " + frameCounter + ": pc: " + Integer.toHexString(crc_check) + " != preon: " + Integer.toHexString(crc));
					for (byte b : frame) System.out.print(Integer.toHexString(((int)b &0xFF)) + " ");
					System.out.println();
				}else{
					
					//Counter check
					preonCounter = (ByteBuffer.wrap(frame, 50, 2).getShort()) & 0xFFFF;
					counter++;
					if (preonCounter != counter){
						if (preonCounter == 0 && counter != 131072) {
							//Zählerumschlag
						}else{
							log += "preon: " + preonCounter + ", internal: " + counter + "\n";
//							if (preonCounter < counter) missingFrames += 131072 - counter + preonCounter;
//							else missingFrames += preonCounter - counter;
						}
						counter = preonCounter;
					}

					//State change
					byte state = (byte) ((frame[POS_STATE] & 0xF0) >> 4);			//lese Statechanges
					if (state > 0){													//Wenn Pin getoggled, dann füge Statechange 
						stateList.add(new State(state,anzMesswerte));				//der Statelist hinzu
						anzStateChanges++;
					}
					
					//TimeoutStatus
					if ((byte) (frame[POS_STATE] & 0xF0) > 0){						//Flag gesetzt, wenn Preon zu lange auf schreiben gewartet hat -> Datenverlust
						communicationError++;
						System.out.println("communication error (" + communicationError + ")");
					}
					
					//Gehe Frame durch und speichere Messwerte
					for (int i=0;i<FRAME_SIZE;i++){
						//Überprüfe ob, frame gültige Messwerte enthält
						if ((frame[2 * i * 3 + POS_I + 2] & 0x0F) == 0 && lastI == 0){	//Messwert am Anfang nicht gültig. Wird übersprungen
							continue;
						}							
						
						values[0] = (((frame[2 * i * 3 + POS_I]	 & 0xFF) << 16) | 	//lese 20 Bit Stromwert
									 ((frame[2 * i * 3 + POS_I + 1] & 0xFF) << 8)  | 
									   frame[2 * i * 3 + POS_I + 2] & 0xF7);
						
						values[0] <<= 8;	//uint24_t zu signed int
						values[0] >>= 8;	//

						//Software oversampling							
						if (os_enable){
							if (os_counter > 1){
								lastI += values[0];
							}else{
								lastI = lastI >> os;
							}
						}else{							
							lastI = values[0];											//speichere letzten gültigen Stromwert							
						}
						//lese 20 Bit Spannungswert
						values[1] = (((frame[2 * i * 3 + POS_U] 	 & 0xFF) << 16) | 
						 		  			 ((frame[2 * i * 3 + POS_U + 1] & 0xFF) << 8)  | 
						 		  			   frame[2 * i * 3 + POS_U + 2] & 0xF0);
						
						values[1] <<= 8;		//uint24_t to signed int
						values[1] >>= 8;		//
						
						if (os_enable){
							if (os_counter > 1){
								lastU += values[1];
								os_counter--;
							}else{
								lastU = lastU >> os;
							}
						}else{
							lastU = values[1];
						}
						
						//Daten speichern		
						if (!os_enable || (os_enable && os_counter == 1)){
							_main.data.addValueToMessdaten(0, values[0], 0, false);
							_main.data.addValueToMessdaten(1, values[1], 0, false);		
							anzMesswerte++;
							
							if (os_enable) os_counter = os;
						}
					}
				}						
				frameCounter++;
			}
			System.out.println("Zusammenfassung:" +
					"\n\tFrames / Messwertpaare erhalten: " + frameCounter + " / " + (frameCounter * FRAME_SIZE) +
//					"\n\tFehlende Frames: " + missingFrames +
					"\n\tCRC-Errors: " + crcErrors + 
					"\n\tvalid Frames: " + (frameCounter - crcErrors) +
					"\n\tcommunication errors: " + communicationError);
			if (!log.isEmpty())System.out.println("Counter-Log:\n" + log);
		}catch (Exception e){
			if (e.getLocalizedMessage().equalsIgnoreCase("closed by user")){
				stop2PortMeasure();
				MeasureFinished(false);
				_main.gui.messungStoppen();
				JOptionPane.showMessageDialog(null, "COM Port wurde unerwartet geschlossen. Programm wird beendet. Bitte neustarten","COM-Port Fehler",JOptionPane.ERROR_MESSAGE);
				_main.exit();
			}else if (e.getMessage().equals("no preamble")){
				stop2PortMeasure();
				MeasureFinished(false);
				_main.gui.messungStoppen();
				JOptionPane.showMessageDialog(null, "Startsequenz nicht erhalten. Messung wurde abgebrochen","USART Fehler",JOptionPane.ERROR_MESSAGE);					
			}
		}
			
	}

	@Override
	public boolean stopMeasure() {
		return false;
	}

	@Override
	public boolean stopMeasure(boolean forceStop) {
		//_main.data.addValueToMessdaten(-1,0,0,false);	//Bei periodischer Messung, schreibe die zuletzt gespeicherten Werte in die CSV Datei 
		stop2PortMeasure();	
		MeasureFinished(true);
		return true;
	}

}
