package net.kodehawa.mantarobot.commands.info;

import lombok.Data;

import java.util.List;

@Data
public class DictionaryData {
	String url = null;
	List<Results> results = null;

	@Data
	public class Results {
		public String headword = null;
		public String part_of_speech = null;
		public List<Pronunciations> pronunciations = null;
		public List<Senses> senses = null;

		@Data
		public class Pronunciations {
			public String ipa = null;
			public String lang = null;
		}

		@Data
		public class Senses {
			public List<String> definition = null;
		}
	}
}
