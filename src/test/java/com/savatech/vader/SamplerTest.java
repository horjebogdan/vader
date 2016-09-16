package com.savatech.vader;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.savatech.vader.Player.PlayerObeserver;

public class SamplerTest {
	@Test
	public void testSampler() throws Exception {
		float s = 0.6f;
		for (int i = 0; i < 3; i++) {
			sample4(i * 4, s, 4, 10, (int) (4));
		}
	}

	private void sample(float playBackSpeed) {
		int frameSize = 2;
		int len = 200;
		int newLen = (int) ((float) len / playBackSpeed);
		byte[] b2 = new byte[newLen];
		for (int ii = 0; ii < b2.length / frameSize; ii++) {
			for (int jj = 0; jj < frameSize; jj++) {
				int to = (ii * frameSize) + jj;
				int from = (int) ((ii * frameSize) * playBackSpeed) + jj;
				System.out.println(to + "=" + from);
			}
		}
	}

	private void sample2(float playBackSpeed) {
		int frameSize = 2;
		int len = 200;
		int newLen = (int) ((float) len / playBackSpeed);
		byte[] b2 = new byte[newLen];
		for (int ii = 0; ii < b2.length / frameSize; ii++) {
			for (int jj = 0; jj < frameSize; jj++) {
				int to = (ii * frameSize) + jj;
				int from = (int) (ii * playBackSpeed) * frameSize + jj;
				System.out.println(to + "=" + from);
			}
		}
	}

	private void sample4(int off, float s, int len, int frameSize, int ar) {

		int fr = len / frameSize;
		int ftor = (int) Math.ceil((fr+1) * s)*frameSize;

		int abr = ((int)(ar/s)/frameSize)*frameSize;

//		int afr = Math.round(speedReadLen / speed) / frameSize;
//		int abr = afr + speedReadLen % frameSize;

		for (int i = 0; i < abr; i++) {
			int foff = i % frameSize;
			int f = i / frameSize;

			int si = (int) (Math.floor(f * s) + foff);
			System.out.println(i+"->"+si);
		}

	}

	@Test
	public void testPlay() throws Exception {
		PlayerObeserver o = new PlayerObeserver() {

			@Override
			public void at(long amicros, long micros) {
				// TODO Auto-generated method stub

			}

		};
		Player sp = new Player(o);
		AtomicInteger ai = new AtomicInteger(10);
		// List<Slice> slices = Files
		// .walk(Paths.get("/Users/bogdanhorje/work/workspaces/marsjee/mindcontrol/vader/data/slices/"))
		// .filter((p) -> ai.decrementAndGet()>0&&p.toString().endsWith("wav")
		// ).map(this::slice).collect(Collectors.toList());

		sp.play(Paths.get("/Users/bogdanhorje/work/workspaces/marsjee/mindcontrol/vader/data/in5.wav"), 0.7f);

		// sp.play(slices, 1.8f);
	}
}
