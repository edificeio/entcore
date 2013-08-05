package edu.one.core.communication.profils;

public class ProfilFactory {

	public Profil getProfil(String profil) {
		switch (profil) {
		case "ELEVE":
			return null;
		default:
			throw new IllegalArgumentException("Invalid profil : " + profil);
		}
	}
}
