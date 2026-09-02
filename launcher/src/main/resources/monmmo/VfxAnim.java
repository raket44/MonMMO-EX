package monmmo;

/**
 * Plays a move's own shipped particle effect. The client's particles.pak holds authored .vfx
 * files for every move id up to 732 (particle/auto/&lt;id&gt;.vfx) - Gen 6 moves included - but the
 * animation registry never gained code entries past 559, so those effects shipped dead and the
 * generic hit played instead.
 *
 * This class is a faithful translation of f.uG1.hQ1() - the client's own generic fallback
 * animation - with the hardcoded generic-hit effect id (729) replaced by the move's id, so the
 * timeline framing (fade-in, tint, screen shake, hit sounds, attacker cry, restore pass) is
 * exactly what the client does around every unregistered move today, but the visual in the middle
 * is the move's real effect. Registered per move by the DexPatch "movevfx" fixup.
 *
 * Compiled at patch time against the client jar, same pipeline as DexPatch itself.
 */
public final class VfxAnim extends f.Dm0 {
  private final short moveId;

  public VfxAnim(f.QL1 attacker, short moveId) {
    super(attacker);
    this.moveId = moveId;
    MapLog.log("VfxAnim constructed for move " + moveId);
  }

  @Override
  public f.Dm0 hQ1() {
    MapLog.log("VfxAnim.hQ1 building move " + moveId);
    try {
      return build();
    } catch (Throwable failure) {
      MapLog.fail(failure);
      throw failure;
    }
  }

  private f.Dm0 build() {
    final float speed = (SW0() ? 1.0f : -1.0f) * f.eR0.IV0;
    f.cd timeline = f.cd.QS0().OR(c30(moveId)).J6();
    yC = timeline;
    timeline.Ix0(JK(0, 0.6f));
    f.cd chain = yC.Ix0(ew(14, 1)).Zy1().J6();
    // Anchor EVERY controller of the effect, not just the first: j00 copies controller <i> with
    // the caster-to-target anchors attached, and self-guards (empty timeline) past the last
    // index. The generic animation anchors only index 0, which is why multi-phase effects -
    // Moonblast carries six controllers - played as a sliver of themselves.
    for (int controller = 0; controller < 12; controller++) {
      chain = chain.Ix0(j00(-1, moveId, controller, 9, 8, 0.0f));
    }
    chain
        .Ix0(Oh(14, 0.5f, 0.0f, 0.5f, f.N31.kA0(31)))
        .Ix0(zF1(14, 1, 2, 0.016f, 0.228f, 1.2f, 1.2f))
        .Ix0(xw1((byte) 2, (short) 1412, 0, 14, 100.0f, 1.0f, gR1))
        .Ix0(xw1((byte) 2, (short) 1440, 0, 14, 950.0f, 1.0f, gR1));
    yC.Zy1()
        .OR(
            f.yl0.kd(
                (index, timelineRef) ->
                    f.Ot.IV0.QH0(
                        (byte) 2, (short) 1, gR1.yQ(), true, speed, 0.6f, 1.0f, 200)))
        .J6()
        .Ix0(ew(14, 0))
        .Ix0(Oh(14, 0.5f, 0.5f, 0.0f, f.N31.kA0(31)))
        .Ix0(zF1(14, 1, 2, 0.016f, 0.228f, 1.0f, 1.0f))
        .Zy1();
    yC.Ix0(Ck0(0.4f, 0.0f));
    yC.Gk(zH0.mp1);
    zH0.gC1(yC);
    return this;
  }
}
