package hexagon.adapter;

import hexagon.port.IObtainPoems;

public class GermanPoetryLibrary implements IObtainPoems {
	public String[] getMePoems() {
		return new String[] {
				"DER PANTHER\nIM JARDIN DES PLANTES, PARIS\n\nSein Blick ist vom Vor�bergehn der St�be\nso m�d geworden, da� er nichts mehr h�lt.\nIhm ist, als ob es tausend St�be g�be\nund hinter tausend St�ben keine Welt.\n\nDer weiche Gang geschmeidig starker Schritte,\nder sich im allerkleinsten Kreise dreht,\nist wie ein Tanz von Kraft um eine Mitte,\nin der bet�ubt ein gro�er Wille steht.\n\nNur manchmal schiebt der Vorhang der Pupille\nsich lautlos auf �. Dann geht ein Bild hinein,\ngeht durch der Glieder angespannte Stille �\nund h�rt im Herzen auf zu sein.\n\n\n--�Der Panther� von Rainer Maria Rilke",
				"Ich sitze am Stra�enrand\nDer Fahrer wechselt das Rad.\nIch bin nicht gern, wo ich herkomme.\nIch bin nicht gern, wo ich hinfahre.\nWarum sehe ich den Radwechsel\nMit Ungeduld?\n\n\n--�Der Radwechsel� von Bertold Brecht" };
	}
}