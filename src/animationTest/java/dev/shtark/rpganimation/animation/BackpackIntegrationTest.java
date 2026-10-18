package dev.shtark.rpganimation.animation;

public final class BackpackIntegrationTest {
    public static void run() {
        check(!EquipTransition.equipped(null,"pack"),"Login baseline must not replay equip");
        check(EquipTransition.equipped("","pack"),"Empty slot to backpack");
        check(!EquipTransition.equipped("pack","pack"),"Inventory changes must not retrigger");
        check(!EquipTransition.equipped("pack",""),"Removal is not equip");
        check(EquipTransition.equipped("pack","upgrade"),"Replacement backpack type");
        check(EquipTransition.equipped("","pack"),"Re-equip after removal");
        var main=new GuiBounds(100,40,176,166);
        check(main.union(new GuiBounds(276,60,90,100)).equals(new GuiBounds(100,40,266,166)),"Right upgrade panel union");
        check(main.union(new GuiBounds(76,35,24,100)).equals(new GuiBounds(76,35,200,171)),"Left upgrade slots union");
        check(main.union(new GuiBounds(0,0,0,0)).equals(main),"Hidden/empty widget ignored");
        check(new GuiBounds(-20,-10,300,220).clipped(256,180).equals(new GuiBounds(0,0,256,180)),"Crop cannot escape framebuffer");
        check(new GuiBounds(300,200,20,20).clipped(256,180).equals(new GuiBounds(256,180,0,0)),"Entirely offscreen crop empty");
        System.out.println("Passed: backpack login/equip/content/removal policy and bounded side-panel crop geometry");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
