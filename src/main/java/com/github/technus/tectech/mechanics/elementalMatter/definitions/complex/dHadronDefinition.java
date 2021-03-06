package com.github.technus.tectech.mechanics.elementalMatter.definitions.complex;

import com.github.technus.tectech.TecTech;
import com.github.technus.tectech.mechanics.elementalMatter.core.cElementalDecay;
import com.github.technus.tectech.mechanics.elementalMatter.core.cElementalDefinitionStackMap;
import com.github.technus.tectech.mechanics.elementalMatter.core.cElementalMutableDefinitionStackMap;
import com.github.technus.tectech.mechanics.elementalMatter.core.stacks.cElementalDefinitionStack;
import com.github.technus.tectech.mechanics.elementalMatter.core.tElementalException;
import com.github.technus.tectech.mechanics.elementalMatter.core.templates.cElementalDefinition;
import com.github.technus.tectech.mechanics.elementalMatter.core.templates.iElementalDefinition;
import com.github.technus.tectech.mechanics.elementalMatter.core.transformations.*;
import com.github.technus.tectech.mechanics.elementalMatter.definitions.primitive.eBosonDefinition;
import com.github.technus.tectech.mechanics.elementalMatter.definitions.primitive.eQuarkDefinition;
import com.github.technus.tectech.thing.item.DebugElementalInstanceContainer_EM;
import com.github.technus.tectech.util.Util;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.github.technus.tectech.compatibility.thaumcraft.elementalMatter.definitions.dComplexAspectDefinition.getNbtTagCompound;
import static com.github.technus.tectech.loader.TecTechConfig.DEBUG_MODE;
import static com.github.technus.tectech.mechanics.elementalMatter.core.transformations.bTransformationInfo.AVOGADRO_CONSTANT_144;
import static com.github.technus.tectech.mechanics.elementalMatter.definitions.complex.dAtomDefinition.TRANSFORMATION_INFO;
import static com.github.technus.tectech.mechanics.elementalMatter.definitions.primitive.eBosonDefinition.boson_Y__;
import static com.github.technus.tectech.thing.metaTileEntity.multi.GT_MetaTileEntity_EM_scanner.*;
import static gregtech.api.enums.OrePrefixes.dust;

/**
 * Created by danie_000 on 17.11.2016.
 */
public final class dHadronDefinition extends cElementalDefinition {//TODO Optimize map i/o
    private final int hash;

    private static final byte nbtType = (byte) 'h';
    //Helpers
    public static final Map<dHadronDefinition,String> SYMBOL_MAP =new HashMap<>();
    public static final Map<dHadronDefinition,String> NAME_MAP =new HashMap<>();
    public static dHadronDefinition hadron_p, hadron_n, hadron_p_, hadron_n_;
    public static cElementalDefinitionStack hadron_p1, hadron_n1, hadron_p2, hadron_n2, hadron_p3, hadron_n3, hadron_p5;
    private static double protonMass = 0D;
    private static double neutronMass = 0D;
    private static final double actualProtonMass=938272081.3D;
    private static final double actualNeutronMass=939565413.3D;

    //float-mass in eV/c^2
    public final double mass;
    //int -electric charge in 1/3rds of electron charge for optimization
    public final int charge;
    public final double rawLifeTime;
    public final int amount;
    //generation max present inside - minus if contains any antiquark
    public final byte type;
    //private final FluidStack fluidThing;
    //private final ItemStack itemThing;

    private final cElementalDefinitionStackMap quarkStacks;

    @Deprecated
    public dHadronDefinition(eQuarkDefinition... quarks) throws tElementalException {
        this(true, new cElementalDefinitionStackMap(quarks));
    }

    @Deprecated
    private dHadronDefinition(boolean check, eQuarkDefinition... quarks) throws tElementalException {
        this(check, new cElementalDefinitionStackMap(quarks));
    }

    public dHadronDefinition(cElementalDefinitionStack... quarks) throws tElementalException {
        this(true, new cElementalDefinitionStackMap(quarks));
    }

    private dHadronDefinition(boolean check, cElementalDefinitionStack... quarks) throws tElementalException {
        this(check, new cElementalDefinitionStackMap(quarks));
    }

    public dHadronDefinition(cElementalDefinitionStackMap quarks) throws tElementalException {
        this(true, quarks);
    }

    private dHadronDefinition(boolean check, cElementalDefinitionStackMap quarks) throws tElementalException {
        if (check && !canTheyBeTogether(quarks)) {
            throw new tElementalException("Hadron Definition error");
        }
        quarkStacks = quarks;

        int amount = 0;
        int charge = 0;
        int type = 0;
        boolean containsAnti = false;
        double mass = 0;
        for (cElementalDefinitionStack quarkStack : quarkStacks.values()) {
            amount += quarkStack.amount;
            if((int)quarkStack.amount!=quarkStack.amount){
                throw new ArithmeticException("Amount cannot be safely converted to int!");
            }
            mass += quarkStack.getMass();
            charge += quarkStack.getCharge();
            type = Math.max(Math.abs(quarkStack.definition.getType()), type);
            if (quarkStack.definition.getType() < 0) {
                containsAnti = true;
            }
        }
        this.amount = amount;
        this.charge = charge;
        this.type = containsAnti ? (byte) -type : (byte) type;
        long mult = this.amount * this.amount * (this.amount - 1);
        mass = mass * 5.543D * mult;//yes it becomes heavier

        if (mass == protonMass && this.amount == 3) {
            rawLifeTime = iElementalDefinition.STABLE_RAW_LIFE_TIME;
            mass=actualProtonMass;
        } else if (mass == neutronMass && this.amount == 3) {
            rawLifeTime = 882D;
            mass=actualNeutronMass;
        } else {
            if (this.amount == 3) {
                rawLifeTime = 1.34D / mass * Math.pow(9.81, charge);
            } else if (this.amount == 2) {
                rawLifeTime = 1.21D / mass / Math.pow(19.80, charge);
            } else {
                rawLifeTime = 1.21D / mass / Math.pow(9.80, charge);
            }
        }
        this.mass=mass;
        hash=super.hashCode();
    }

    //public but u can just try{}catch(){} the constructor it still calls this method
    private static boolean canTheyBeTogether(cElementalDefinitionStackMap stacks) {
        long amount = 0;
        for (cElementalDefinitionStack quarks : stacks.values()) {
            if (!(quarks.definition instanceof eQuarkDefinition)) {
                return false;
            }
            if((int)quarks.amount!=quarks.amount){
                throw new ArithmeticException("Amount cannot be safely converted to int!");
            }
            amount += quarks.amount;
        }
        return amount >= 2 && amount <= 12;
    }

    @Override
    public String getName() {
        StringBuilder name= new StringBuilder(getSimpleName());
        name.append(':');
        String sym= NAME_MAP.get(this);
        if(sym!=null){
            name.append(' ').append(sym);
        }else {
            for (cElementalDefinitionStack quark : quarkStacks.values()) {
                name.append(' ').append(quark.definition.getSymbol()).append((int)quark.amount);
            }
        }
        return name.toString();
    }

    private String getSimpleName() {
        switch (amount) {
            case 2:
                return "Meson";
            case 3:
                return "Baryon";
            case 4:
                return "Tetraquark";
            case 5:
                return "Pentaquark";
            case 6:
                return "Hexaquark";
            default:
                return "Hadron";
        }
    }

    @Override
    public String getSymbol() {
        String sym=SYMBOL_MAP.get(this);
        if(sym!=null){
            return sym;
        }else {
            StringBuilder symbol = new StringBuilder(8);
            for (cElementalDefinitionStack quark : quarkStacks.values()) {
                for (int i = 0; i < quark.amount; i++) {
                    symbol.append(quark.definition.getSymbol());
                }
            }
            return symbol.toString();
        }
    }

    @Override
    public String getShortSymbol() {
        String sym=SYMBOL_MAP.get(this);
        if(sym!=null){
            return sym;
        }else {
            StringBuilder symbol = new StringBuilder(8);
            for (cElementalDefinitionStack quark : quarkStacks.values()) {
                for (int i = 0; i < quark.amount; i++) {
                    symbol.append(quark.definition.getShortSymbol());
                }
            }
            return symbol.toString();
        }
    }

    @Override
    public byte getColor() {
        return -7;
    }

    @Override
    public cElementalDefinitionStackMap getSubParticles() {
        return quarkStacks;
    }

    @Override
    public cElementalDecay[] getNaturalDecayInstant() {
        cElementalDefinitionStack[] quarkStacks = this.quarkStacks.values();
        if (amount == 2 && quarkStacks.length == 2 && quarkStacks[0].definition.getMass() == quarkStacks[1].definition.getMass() && quarkStacks[0].definition.getType() == -quarkStacks[1].definition.getType()) {
            return cElementalDecay.noProduct;
        }
        ArrayList<cElementalDefinitionStack> decaysInto = new ArrayList<>();
        for (cElementalDefinitionStack quarks : quarkStacks) {
            if (quarks.definition.getType() == 1 || quarks.definition.getType() == -1) {
                //covers both quarks and antiquarks
                decaysInto.add(quarks);
            } else {
                //covers both quarks and antiquarks
                decaysInto.add(new cElementalDefinitionStack(boson_Y__, 2));
            }
        }
        return new cElementalDecay[]{
                new cElementalDecay(0.75D, decaysInto.toArray(new cElementalDefinitionStack[0])),
                eBosonDefinition.deadEnd
        };
    }

    @Override
    public cElementalDecay[] getEnergyInducedDecay(long energyLevel) {
        cElementalDefinitionStack[] quarkStacks = this.quarkStacks.values();
        if (amount == 2 && quarkStacks.length == 2 && quarkStacks[0].definition.getMass() == quarkStacks[1].definition.getMass() && quarkStacks[0].definition.getType() == -quarkStacks[1].definition.getType()) {
            return cElementalDecay.noProduct;
        }
        return new cElementalDecay[]{new cElementalDecay(0.75D, quarkStacks), eBosonDefinition.deadEnd}; //decay into quarks
    }

    @Override
    public double getEnergyDiffBetweenStates(long currentEnergyLevel, long newEnergyLevel) {
        return iElementalDefinition.DEFAULT_ENERGY_REQUIREMENT *(newEnergyLevel-currentEnergyLevel);
    }

    @Override
    public boolean usesSpecialEnergeticDecayHandling() {
        return false;
    }

    @Override
    public boolean usesMultipleDecayCalls(long energyLevel) {
        return false;
    }

    @Override
    public boolean decayMakesEnergy(long energyLevel) {
        return false;
    }

    @Override
    public boolean fusionMakesEnergy(long energyLevel) {
        return false;
    }

    @Override
    public cElementalDecay[] getDecayArray() {
        cElementalDefinitionStack[] quarkStacks = this.quarkStacks.values();
        if (amount == 2 && quarkStacks.length == 2 &&
                quarkStacks[0].definition.getMass() == quarkStacks[1].definition.getMass() &&
                quarkStacks[0].definition.getType() == -quarkStacks[1].definition.getType()) {
            return cElementalDecay.noProduct;
        } else if (amount != 3) {
            return new cElementalDecay[]{new cElementalDecay(0.95D, quarkStacks), eBosonDefinition.deadEnd}; //decay into quarks
        } else {
            ArrayList<eQuarkDefinition> newBaryon = new ArrayList<>();
            iElementalDefinition[] Particles = new iElementalDefinition[2];
            for (cElementalDefinitionStack quarks : quarkStacks) {
                for (int i = 0; i < quarks.amount; i++) {
                    newBaryon.add((eQuarkDefinition) quarks.definition);
                }
            }
            //remove last
            eQuarkDefinition lastQuark = newBaryon.remove(2);

            cElementalDefinitionStack[] decay;
            if (Math.abs(lastQuark.getType()) > 1) {
                decay = lastQuark.getDecayArray()[1].outputStacks.values();
            } else {
                decay = lastQuark.getDecayArray()[2].outputStacks.values();
            }
            newBaryon.add((eQuarkDefinition) decay[0].definition);
            Particles[0] = decay[1].definition;
            Particles[1] = decay[2].definition;

            eQuarkDefinition[] contentOfBaryon = newBaryon.toArray(new eQuarkDefinition[3]);

            try {
                return new cElementalDecay[]{
                        new cElementalDecay(0.001D, new dHadronDefinition(false, contentOfBaryon), Particles[0], Particles[1], boson_Y__),
                        new cElementalDecay(0.99D, new dHadronDefinition(false, contentOfBaryon), Particles[0], Particles[1]),
                        eBosonDefinition.deadEnd};
            } catch (tElementalException e) {
                if (DEBUG_MODE) {
                    e.printStackTrace();
                }
                return new cElementalDecay[]{eBosonDefinition.deadEnd};
            }
        }
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public int getCharge() {
        return charge;
    }

    @Override
    public double getRawTimeSpan(long currentEnergy) {
        return rawLifeTime;
    }

    @Override
    public boolean isTimeSpanHalfLife() {
        return true;
    }

    @Override
    public byte getType() {
        return type;
    }

    //@Override
    //public iElementalDefinition getAnti() {
    //    cElementalDefinitionStack[] stacks = this.quarkStacks.values();
    //    cElementalDefinitionStack[] antiElements = new cElementalDefinitionStack[stacks.length];
    //    for (int i = 0; i < antiElements.length; i++) {
    //        antiElements[i] = new cElementalDefinitionStack(stacks[i].definition.getAnti(), stacks[i].amount);
    //    }
    //    try {
    //        return new dHadronDefinition(false, antiElements);
    //    } catch (tElementalException e) {
    //        if (DEBUG_MODE) e.printStackTrace();
    //        return null;
    //    }
    //}

    @Override
    public iElementalDefinition getAnti() {
        cElementalMutableDefinitionStackMap anti = new cElementalMutableDefinitionStackMap();
        for (cElementalDefinitionStack stack : quarkStacks.values()) {
            anti.putReplace(new cElementalDefinitionStack(stack.definition.getAnti(), stack.amount));
        }
        try {
            return new dHadronDefinition(anti.toImmutable_optimized_unsafeLeavesExposedElementalTree());
        } catch (tElementalException e) {
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public aFluidDequantizationInfo someAmountIntoFluidStack() {
        return null;
    }

    @Override
    public aItemDequantizationInfo someAmountIntoItemsStack() {
        return null;
    }

    @Override
    public aOredictDequantizationInfo someAmountIntoOredictStack() {
        return null;
    }

    @Override
    public NBTTagCompound toNBT() {
        return getNbtTagCompound(nbtType, quarkStacks);
    }

    public static dHadronDefinition fromNBT(NBTTagCompound nbt) {
        cElementalDefinitionStack[] stacks = new cElementalDefinitionStack[nbt.getInteger("i")];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = cElementalDefinitionStack.fromNBT(nbt.getCompoundTag(Integer.toString(i)));
        }
        try {
            return new dHadronDefinition(stacks);
        } catch (tElementalException e) {
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static void run() {
        try {
            hadron_p = new dHadronDefinition(new cElementalDefinitionStackMap(eQuarkDefinition.quark_u.getStackForm(2), eQuarkDefinition.quark_d.getStackForm(1)));
            protonMass = hadron_p.mass;
            //redefine the proton with proper lifetime (the lifetime is based on mass comparison)
            hadron_p = new dHadronDefinition(new cElementalDefinitionStackMap(eQuarkDefinition.quark_u.getStackForm(2), eQuarkDefinition.quark_d.getStackForm(1)));
            SYMBOL_MAP.put(hadron_p,"p");
            NAME_MAP.put(hadron_p,"Proton");
            DebugElementalInstanceContainer_EM.STACKS_REGISTERED.add(hadron_p);
            hadron_p_ = (dHadronDefinition) hadron_p.getAnti();
            SYMBOL_MAP.put(hadron_p_,"~p");
            NAME_MAP.put(hadron_p_,"Anti Proton");
            DebugElementalInstanceContainer_EM.STACKS_REGISTERED.add(hadron_p_);
            hadron_n = new dHadronDefinition(new cElementalDefinitionStackMap(eQuarkDefinition.quark_u.getStackForm(1), eQuarkDefinition.quark_d.getStackForm(2)));
            neutronMass = hadron_n.mass;
            //redefine the neutron with proper lifetime (the lifetime is based on mass comparison)
            hadron_n = new dHadronDefinition(new cElementalDefinitionStackMap(eQuarkDefinition.quark_u.getStackForm(1), eQuarkDefinition.quark_d.getStackForm(2)));
            SYMBOL_MAP.put(hadron_n, "n");
            NAME_MAP.put(hadron_n, "Neutron");
            DebugElementalInstanceContainer_EM.STACKS_REGISTERED.add(hadron_n);
            hadron_n_ = (dHadronDefinition) hadron_n.getAnti();
            SYMBOL_MAP.put(hadron_n_,"~n");
            NAME_MAP.put(hadron_n_,"Anti Neutron");
            DebugElementalInstanceContainer_EM.STACKS_REGISTERED.add(hadron_n_);
        } catch (tElementalException e) {
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
            protonMass = -1;
            neutronMass = -1;
        }
        hadron_p1 = new cElementalDefinitionStack(hadron_p, 1D);
        hadron_n1 = new cElementalDefinitionStack(hadron_n, 1D);
        hadron_p2 = new cElementalDefinitionStack(hadron_p, 2D);
        hadron_n2 = new cElementalDefinitionStack(hadron_n, 2D);
        hadron_p3 = new cElementalDefinitionStack(hadron_p, 3D);
        hadron_n3 = new cElementalDefinitionStack(hadron_n, 3D);
        hadron_p5 = new cElementalDefinitionStack(hadron_p, 5D);

        try {
            cElementalDefinition.addCreatorFromNBT(nbtType, dHadronDefinition.class.getMethod("fromNBT", NBTTagCompound.class),(byte)-64);
        } catch (Exception e) {
            if (DEBUG_MODE) {
                e.printStackTrace();
            }
        }
        if(DEBUG_MODE) {
            TecTech.LOGGER.info("Registered Elemental Matter Class: Hadron " + nbtType + ' ' + -64);
        }
    }

    public static void setTransformations(){
        //Added to atom map, but should be in its own
        cElementalDefinitionStack neutrons=new cElementalDefinitionStack(hadron_n, 1000* AVOGADRO_CONSTANT_144);
        TRANSFORMATION_INFO.oredictDequantization.put(neutrons.definition,new aOredictDequantizationInfo(neutrons, dust, Materials.Neutronium,1));
        bTransformationInfo.oredictQuantization.put(
                OreDictionary.getOreID(OrePrefixes.ingotHot.name()+Materials.Neutronium.mName),
                new aOredictQuantizationInfo(OrePrefixes.ingotHot,Materials.Neutronium,1 ,neutrons)
        );
    }

    @Override
    public byte getClassType() {
        return -64;
    }

    public static byte getClassTypeStatic(){
        return -64;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public void addScanShortSymbols(ArrayList<String> lines, int capabilities, long energyLevel) {
        if(Util.areBitsSet(SCAN_GET_NOMENCLATURE|SCAN_GET_CHARGE|SCAN_GET_MASS|SCAN_GET_TIMESPAN_INFO, capabilities)) {
            lines.add(getShortSymbol());
        }
    }

    @Override
    public void addScanResults(ArrayList<String> lines, int capabilities, long energyLevel) {
        if(Util.areBitsSet(SCAN_GET_CLASS_TYPE, capabilities)) {
            lines.add("CLASS = " + nbtType + ' ' + getClassType());
        }
        if(Util.areBitsSet(SCAN_GET_NOMENCLATURE|SCAN_GET_CHARGE|SCAN_GET_MASS|SCAN_GET_TIMESPAN_INFO, capabilities)) {
            lines.add("NAME = "+getSimpleName());
            //lines.add("SYMBOL = "+getSymbol());
        }
        if(Util.areBitsSet(SCAN_GET_CHARGE,capabilities)) {
            lines.add("CHARGE = " + getCharge() / 3D + " e");
        }
        if(Util.areBitsSet(SCAN_GET_COLOR,capabilities)) {
            lines.add(getColor() < 0 ? "COLORLESS" : "CARRIES COLOR");
        }
        if(Util.areBitsSet(SCAN_GET_MASS,capabilities)) {
            lines.add("MASS = " + getMass() + " eV/c\u00b2");
        }
        if(Util.areBitsSet(SCAN_GET_TIMESPAN_INFO, capabilities)){
            lines.add("HALF LIFE = "+getRawTimeSpan(energyLevel)+ " s");
            lines.add("    "+"At current energy level");
        }
    }
}
