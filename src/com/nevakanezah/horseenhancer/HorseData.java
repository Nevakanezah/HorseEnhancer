package com.nevakanezah.horseenhancer;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityBreedEvent;
import com.nevakanezah.horseenhancer.util.NameConverter;

import net.md_5.bungee.api.ChatColor;

public class HorseData implements java.io.Serializable {

	private static final long serialVersionUID = -5063530984002853922L;
	
	private static final String UNKNOWN = "Unknown";
	
	private UUID uniqueID;
	private String horseID;
	
	private UUID fatherID;
	private String fatherName;
	
	private UUID motherID;
	private String motherName;
	
	private Gender gender;
	private EntityType type;

	private enum Gender {
		STALLION, // Male horse capable of breeding
		GELDING, // Male animal that is unable to breed
		MULE, // I doubt anyone cares what gender their mule is
		MARE, // Female horse
		JENNY, // Female donkey
		JACK, // Male donkey capable of breeding
		DAM, // Female llama
		HERDSIRE, // Male llama capable of breeding
		UNDEAD, // Zombie or skeleton
		INBRED, // Child of related parents
		CREATIVE, // Creative-mode spawned horses with unique breeding behaviour
		UNIQUE // Special case, or custom behaviour
	}
	
	// CONSTRUCTORS
	
	public HorseData(Entity child, Entity father, Entity mother, double bias) {
		
		if(child == null) return;
		if(!(child instanceof AbstractHorse)) return;
		
		initialize(child, father, mother);
		this.gender = generateGender(child, bias);
	}
	
	public HorseData(UUID childID, UUID fatherID, UUID motherID, double bias) {
		
		if(childID == null || fatherID == null || motherID == null) return;
		
		Entity child = Bukkit.getEntity(childID);
		if(!(child instanceof AbstractHorse)) return;
		
		Entity father = Bukkit.getEntity(fatherID);
		Entity mother = Bukkit.getEntity(motherID);
		
		initialize(child, father, mother);
		this.gender = generateGender(child, bias);
	}
	
	public HorseData(EntityBreedEvent evt, double bias) {
		
		if(evt == null) return;
		if(!(evt.getEntity() instanceof AbstractHorse)) return;
		
		Entity child = evt.getEntity();
		Entity father = evt.getFather();
		Entity mother = evt.getMother();
		
		if(child == null || father == null || mother == null) return;		
		
		initialize(child, father, mother);
		this.gender = generateGender(child, bias);
	}
	
	private void initialize(Entity child, Entity father, Entity mother) {	
		if(child == null)
			return;
		
		this.setUniqueID(child.getUniqueId());
		generateHorseID((int)child.getUniqueId().getLeastSignificantBits());
		this.fatherID = (father != null) ? father.getUniqueId() : null;
		this.motherID = (mother != null) ? mother.getUniqueId() : null;
		StringBuilder parentName = new StringBuilder();
		
		if(fatherID == null)
			this.fatherName = ChatColor.BLUE + UNKNOWN;
		else {
			NameConverter.uint2quint(parentName, Math.abs((int)father.getUniqueId().getLeastSignificantBits()), '-');
			
			String fatherNametag = ChatColor.BLUE + "#" + parentName;
			if(father.getCustomName() != null)
				fatherNametag = ChatColor.GREEN + father.getCustomName() + " " + fatherNametag;
			this.fatherName = fatherNametag;
			parentName.delete(0, parentName.length());
		}
			
		if(motherID == null)
			this.motherName = ChatColor.BLUE + UNKNOWN;
		else {
			NameConverter.uint2quint(parentName, Math.abs((int)mother.getUniqueId().getLeastSignificantBits()), '-');

			String motherNametag = ChatColor.BLUE + "#" + parentName;
			if(mother.getCustomName() != null)
				motherNametag = ChatColor.GREEN + mother.getCustomName() + " " + motherNametag;
			this.motherName = motherNametag;
			parentName.delete(0, parentName.length());
		}
		
		this.setType(child.getType());
	}
	
	// UTILITY FUNCTIONS
	
	public boolean geld() {
		if(!(gender.equals(Gender.STALLION) || gender.equals(Gender.JACK) || gender.equals(Gender.HERDSIRE))) return false;
		
		this.gender = Gender.GELDING;
		return true;
	}
	
	private Gender generateGender(Entity horse, double bias) {
		
		if(horse == null) return null;
		if(!(horse instanceof AbstractHorse)) return null;
		
		double rand = Math.random();
		
		switch(horse.getType()){
		case HORSE:
			return (rand < bias) ? Gender.STALLION : Gender.MARE;
		case LLAMA:
			return (rand < bias) ? Gender.HERDSIRE : Gender.DAM;
		case DONKEY:
			return (rand < bias) ? Gender.JACK : Gender.JENNY;
		case MULE:
			return Gender.MULE;
		case SKELETON_HORSE:
			return Gender.UNDEAD;
		case ZOMBIE_HORSE:
			return Gender.UNDEAD;
		default:
			break;
		}
		
		return null;
	}
	
	/**
	 * Check if this horse is physiologically capable of reproducing with the provided mate
	 * @param mate The data container of the prospective mate
	 * @return true if mating is possible, false otherwise
	 */
	public boolean genderCompatible(HorseData mate) {
		Gender mateGender = mate.getGender();
		
		switch(gender) {
		case STALLION:
			return (mateGender.equals(Gender.MARE) || mateGender.equals(Gender.JENNY));
		case MARE:
			return (mateGender.equals(Gender.STALLION) || mateGender.equals(Gender.JACK));
		case JENNY:
			return (mateGender.equals(Gender.STALLION) || mateGender.equals(Gender.JACK));
		case JACK:
			return (mateGender.equals(Gender.MARE) || mateGender.equals(Gender.JENNY));
		case DAM:
			return (mateGender.equals(Gender.HERDSIRE));
		case HERDSIRE:
			return (mateGender.equals(Gender.DAM));
		default:
			return false;
		}
	}
	
	public boolean canSire() {
		switch(gender) {
		case STALLION:
			return true;
		case JACK:
			return true;
		case HERDSIRE:
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Compare with another HorseDataContainer to see if the two horses are related. Mainly used for incest check.
	 * @param partner The father's datacontainer
	 * @return true if the horses are related.
	 */
	public boolean isRelated(HorseData partner) {
		
		UUID pFather = partner.getFatherID();
		UUID pMother = partner.getMotherID();
		UUID partnerID = partner.getUniqueID();
		boolean inbred = false;

		if(partnerID.equals(fatherID) || partnerID.equals(motherID)) // Are they your parent?
			inbred = true;
		if(pFather != null && pFather.equals(uniqueID)) // Are you theirs?
			inbred = true;
		if(pMother != null && pMother.equals(uniqueID))
			inbred = true;
		
		 // Direct siblings
		if(motherID != null && fatherID != null
			&& motherID.equals(pMother) && fatherID.equals(pFather))
					inbred = true;
		
		return inbred;
	}
	
	// GETTERS AND SETTERS
	
	public String getHorseID() {
		if(this.horseID == null)
			generateHorseID((int)uniqueID.getLeastSignificantBits());
		return horseID;
	}

	public void setHorseID(String horseID) {
		this.horseID = horseID;
	}
	
	public void generateHorseID(int horseID) {
		StringBuilder name = new StringBuilder();
		NameConverter.uint2quint(name, Math.abs(horseID), '-');
		
		this.horseID = name.toString();
	}

	public UUID getFatherID() {
		return fatherID;
	}

	public void setFatherID(UUID fatherID) {
		this.fatherID = fatherID;
	}

	public String getFatherName() {
		return fatherName;
	}

	public void setFatherName(String fatherName) {
		this.fatherName = fatherName;
	}

	public UUID getMotherID() {
		return motherID;
	}

	public void setMotherID(UUID motherID) {
		this.motherID = motherID;
	}

	public String getMotherName() {
		return motherName;
	}

	public void setMotherName(String motherName) {
		this.motherName = motherName;
	}

	public UUID getUniqueID() {
		return uniqueID;
	}

	public void setUniqueID(UUID childID) {
		this.uniqueID = childID;
	}

	public Gender getGender() {
		return gender;
	}
	
	public String getGenderName() {
		return gender.toString();
	}

	public void setGender(String input) {
		this.gender = Gender.valueOf(input);
	}
	
	public EntityType getType() {
		return type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}
	
	public void setMother(Entity mother) {
		this.motherID = mother.getUniqueId();
		this.motherName = mother.getCustomName() != null ? mother.getCustomName() : ChatColor.BLUE + UNKNOWN; 
	}
	
	public void setFather(Entity father) {
		this.fatherID = father.getUniqueId();
		this.fatherName = father.getCustomName() != null ? father.getCustomName() : ChatColor.BLUE + UNKNOWN;
	}
}
