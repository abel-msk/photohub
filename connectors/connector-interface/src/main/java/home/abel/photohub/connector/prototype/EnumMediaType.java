package home.abel.photohub.connector.prototype;

public enum EnumMediaType {

	//ENUM_VAL   MIME_BASE_TYPE
	UNKNOWN    ("unknown"),
	IMAGE      ("image"),
	AUDIO      ("audio"),
	THUMB      ("image"),
	VIDEO      ("video"),
	IMAGE_FILE ("image"),
	IMAGE_NET  ("image"),
	VIDEO_FILE ("video"),
	VIDEO_NET  ("video"),
	AUDIO_TYPE ("audio"),
	AUDIO_NET  ("audio"),
	THUMB_NET  ("image"),
	THUMB_FILE ("image"),
	ACC_LOACL  ("unknown"),
	ACC_NET    ("unknown")
	;

	private final String baseType;
	EnumMediaType(String baseType) {
		this.baseType = baseType;
	}

	public String getBaseTypeAsStr() {
		return baseType;
	}

	public EnumMediaType getBaseType() {
		return EnumMediaType.valueOf(baseType);
	}

}
