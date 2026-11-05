package tools;

public class MyMath {

	public static String roundString(float d, int pos){
		double pot = Math.pow(10, pos);
		double temp = Math.round(d * pot) / pot;
		return String.format("%.3f", temp);
	}
	
	public static float round(float d, int pos){
		float pot = (float) Math.pow(10, pos);
		double temp = Math.round(d * pot) / pot;
		return (float) temp;
	}
	
	public static int roundInt(long l, int pot, boolean aufrunden){
		if (l == 0 && aufrunden) return 1; 
		float p = (float) Math.pow(10, pot);
		if (aufrunden) return (int) (((int)(l / p) + 1) * p);
		return (int) (((int)(l / p)) * p);
	}
	
	public static byte getRangeID(float max, float min){
		//Eingehende Werte in A/V
		float d = max - min;
		float absVal = Math.abs(max - min);
		
		if (d == 0) return -1;
		if (d >= 1000){										// d > 1x10^1
			return 0;
		}else if (absVal < 1000 && absVal >= 0.001){
			return 1;
		}else if (absVal < 0.001 && absVal >= 0.000001){		// 1x10^-6 < d < 1x10^-3
			return 2;			
		}else if (absVal < 0.000001){		// 1x10^-6 < d < 1x10^-3
			return 3;			
		}
		return 0;		
	}
		
	/* CITT CRC16 polynomial ^16 + ^12 + ^5 + 1 */
	/*---------------------------------------------------------------------------*/
	private static int crc16_add(byte b, int acc){
//	  System.out.println("----------------------------------\n");
//	  System.out.println("acc = " + Integer.toHexString(acc) + ", b = " + Integer.toHexString(b));
	  acc ^= b & 0xFF;
//	  System.out.println("acc ^= b: " + Integer.toHexString(acc));
	  acc = (((acc & 0xffff) >> 8) | ((acc & 0xffff) << 8)  & 0xffff);
//	  System.out.println("acc = (((acc & 0xffff) >> 8) | ((acc & 0xffff) << 8)  & 0xffff): " + Integer.toHexString(acc));
	  acc ^= ((acc & 0xff00) << 4) & 0xffff;
//	  System.out.println("acc ^= ((acc & 0xff00) << 4) & 0xffff: " + Integer.toHexString(acc));
	  acc ^= (((acc & 0xffff) >> 8) >> 4  & 0xffff);
//	  System.out.println("acc ^= (((acc & 0xffff) >> 8) >> 4 & 0xffff): " + Integer.toHexString(acc));
	  acc ^= (((acc & 0xffff) & 0xff00) >> 5) & 0xffff;
//	  System.out.println("acc ^= (((acc & 0xffff) & 0xff00) >> 5) & 0xffff: " + Integer.toHexString(acc));
	  return acc;
	}

	/*---------------------------------------------------------------------------*/
	public static int crc16_data(byte[] data, int acc){
	  int i;

	  for(i = 0; i < data.length; ++i) {
	    acc = crc16_add(data[i], acc);
	  }
	  return acc;
	}
	
//	public static void main (String[] args){
////		byte[] out = {0x34,0x28,0x23,0x74,(byte) 0x95,(byte) 0xe6,0x17,(byte) 0xF8};
//		byte[] out = {  2,  2, 30,(byte) 177, 29,(byte) 197,  8, 47 };
//		int checksum = crc16_data(out, 0);
//		System.out.println(Integer.toHexString(checksum));
//
//	}
//

}
