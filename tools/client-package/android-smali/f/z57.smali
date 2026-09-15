.class public final synthetic Lf/z57;
.super Ljava/lang/Object;
.source "r8-map-id-21a863b15956229bafbacbd9400e9628b6470120c5fc77a80c1de31b2e861762"

# interfaces
.implements Lf/oa1;


# instance fields
.field public final synthetic CL1:I

.field public final synthetic O91:Lf/jd6;


# direct methods
.method public synthetic constructor <init>(Lf/jd6;I)V
    .registers 3

    .line 1
    iput p2, p0, Lf/z57;->CL1:I

    .line 2
    .line 3
    iput-object p1, p0, Lf/z57;->O91:Lf/jd6;

    .line 4
    .line 5
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 6
    .line 7
    .line 8
    return-void
.end method


# virtual methods
.method public final Gu0()V
    # MonMMO-EX 2026-09-15: .registers 3 -> 5 (p0 becomes v4; v2/v3 are the new temporaries) so the
    # team-wide stat table (kind 17) anchors its status/up|down effects CASTER -> ENEMY exactly like
    # the per-monster path in f/i90.tn0 - see the comment there. jd6.Ic1 made the caster and the
    # target the same monster, so the caster->enemy branch zeroes out and update() uses the
    # (0,1,0) axis x strength: down falls, up rises.
    .registers 5

    .line 1
    iget v0, p0, Lf/z57;->CL1:I

    .line 2
    .line 3
    iget-object v1, p0, Lf/z57;->O91:Lf/jd6;

    .line 4
    .line 5
    packed-switch v0, :pswitch_data_1c

    .line 6
    .line 7
    .line 8
    const-string v0, "status/down"

    .line 9
    .line 10
    invoke-virtual {v1, v0}, Lf/cj1;->bl1(Ljava/lang/String;)Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;

    .line 11
    .line 12
    .line 13
    move-result-object v0

    sget-object v2, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->CASTER:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    sget-object v3, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->ENEMY:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    invoke-virtual {v0, v2, v3}, Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;->setAllControllersTargets(Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;)V

    .line 14
    invoke-virtual {v1, v0}, Lf/cj1;->KZ1(Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;)V

    .line 15
    .line 16
    .line 17
    return-void

    .line 18
    :pswitch_11
    const-string v0, "status/up"

    .line 19
    .line 20
    invoke-virtual {v1, v0}, Lf/cj1;->bl1(Ljava/lang/String;)Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;

    .line 21
    .line 22
    .line 23
    move-result-object v0

    sget-object v2, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->CASTER:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    sget-object v3, Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;->ENEMY:Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;
    invoke-virtual {v0, v2, v3}, Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;->setAllControllersTargets(Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;Lcom/badlogic/gdx/graphics/g3d/particles/TargetType;)V

    .line 24
    invoke-virtual {v1, v0}, Lf/cj1;->KZ1(Lcom/badlogic/gdx/graphics/g3d/particles/ParticleEffectExt;)V

    .line 25
    .line 26
    .line 27
    return-void

    .line 28
    nop

    .line 29
    :pswitch_data_1c
    .packed-switch 0x0
        :pswitch_11
    .end packed-switch
.end method
