package com.shubham.ir.project.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shubham.ir.project.dataformat.Tweet;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

@Service
public class TweetFormatService {

	@Value("${format.emoticons.file.location}")
	private String emoticonsFile;

	@Value("${format.kaomojis.file.location}")
	private String kaomojisFile;

	@Value("${format.tweet.repository.location}")
	private String tweetRepo;

	@Value("${format.tweet.output.location}")
	private String formattedTweetOutputLocation;

	final static Logger logger = Logger.getLogger(TweetFormatService.class);

	public void formatTweets() throws IOException {
		List<Emoji> allEmojis = new ArrayList<Emoji>();
		allEmojis.addAll(EmojiManager.getAll());
		List<String> allEmojisAsUnicode = new ArrayList<String>();

		for (Emoji em : allEmojis) {
			allEmojisAsUnicode.add(em.getUnicode());
		}

		List<String> allEmoticons = FileUtils.readLines(new File(emoticonsFile), StandardCharsets.UTF_8);
		List<String> allKaomojis = FileUtils.readLines(new File(kaomojisFile), StandardCharsets.UTF_8);

		Collection<File> allFiles = FileUtils.listFiles(new File(tweetRepo), null, false);
		for (File tweetFile : allFiles) {
			File outputFile = new File(formattedTweetOutputLocation + tweetFile.getName());
			FileUtils.write(outputFile, "", StandardCharsets.UTF_8, false);
			List<String> allTweets = FileUtils.readLines(tweetFile, StandardCharsets.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			SimpleDateFormat from = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
			SimpleDateFormat toLeft = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ssZ", Locale.US);
			toLeft.setTimeZone(TimeZone.getTimeZone("GMT"));
			for (String tweetedString : allTweets) {
				Tweet tweet = mapper.readValue(tweetedString, Tweet.class);
				/*
				 * StringBuilder locationReversal = new StringBuilder(); String
				 * location = tweet.getTweet_loc(); if
				 * (!StringUtils.isEmpty(location)) { String[] latLong =
				 * location.split(","); locationReversal.append(latLong[1]);
				 * locationReversal.append(",");
				 * locationReversal.append(latLong[0]);
				 * tweet.setTweet_loc(locationReversal.toString()); }
				 */

				Date date = null;
				try {
					date = from.parse(tweet.getTweet_date());
				} catch (ParseException e) {
					e.printStackTrace();
				}
				// TODO : Change this implementation
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);

				StringBuilder dateBuilder = new StringBuilder();
				dateBuilder.append(cal.get(Calendar.YEAR));
				dateBuilder.append("-");
				int month = cal.get(Calendar.MONTH) + 1;
				if (month < 10) {
					dateBuilder.append("0");
				}
				dateBuilder.append(month);
				dateBuilder.append("-");
				int day = cal.get(Calendar.DAY_OF_MONTH);
				if (day < 10) {
					dateBuilder.append("0");
				}
				dateBuilder.append(day);
				dateBuilder.append("T");

				int hour = cal.get(Calendar.HOUR_OF_DAY);
				if (hour < 10) {
					dateBuilder.append("0");
				}
				dateBuilder.append(hour);
				dateBuilder.append(":");
				int min = cal.get(Calendar.MINUTE);
				if (min < 10) {
					dateBuilder.append("0");
				}
				dateBuilder.append(min);
				dateBuilder.append(":");
				int sec = cal.get(Calendar.SECOND);

				if (sec < 10) {
					dateBuilder.append("0");
				}
				dateBuilder.append(sec);
				dateBuilder.append("Z");
				// dateBuilder.append(toLeft.format(date));

				tweet.setTweet_date(dateBuilder.toString());

				String input = tweet.getTweet_text();
				String modifiedInputString = EmojiParser.parseToUnicode(input);
				List<String> allEmoticonsEtcInTweet = new ArrayList<String>();
				for (String uniEm : allEmojisAsUnicode) {
					if (modifiedInputString.contains(uniEm)) {
						allEmoticonsEtcInTweet.add(uniEm);
						input = input.replaceAll(uniEm, "");
					}
				}
				input = EmojiParser.removeAllEmojis(input);

				for (String emoticon : allEmoticons) {
					if (input.contains(emoticon)) {

						if (emoticon.contains(")")) {
							input = input.replaceAll(emoticon.replaceAll("\\)", "\\\\)"), "");
						} else if (emoticon.contains("}")) {
							input = input.replaceAll(emoticon.replaceAll("\\}", "\\\\}"), "");
						}

						else if (emoticon.contains("(")) {
							input = input.replaceAll(emoticon.replaceAll("\\(", "\\\\("), "");
						} else if (emoticon.contains("{")) {
							input = input.replaceAll(emoticon.replaceAll("\\{", "\\\\{"), "");
						} else if (emoticon.contains("[")) {
							input = input.replaceAll(emoticon.replaceAll("\\[", "\\\\["), "");
						} else if (emoticon.contains("]")) {
							input = input.replaceAll(emoticon.replaceAll("\\]", "\\\\]"), "");
						}

						else {
							input = input.replaceAll(emoticon, "");
						}

						allEmoticonsEtcInTweet.add(emoticon.trim());
					}
				}

				for (String kaomoji : allKaomojis) {
					if (input.contains(kaomoji)) {
						if (kaomoji.contains("(") && kaomoji.contains("")) {
							String repKaomoji = kaomoji.replaceAll("\\)", "\\\\)");
							repKaomoji = repKaomoji.replaceAll("\\(", "\\\\(");
							input = input.replaceAll(repKaomoji, "");
						} else if (kaomoji.contains(")")) {
							try {
								input = input.replaceAll(kaomoji.replaceAll("\\)", "\\\\)"), "");
							} catch (Exception e) {
								logger.error(e);
								logger.debug(input);
								logger.debug("Kaomoji : " + kaomoji);
								logger.debug("Replaced Kaomoji : " + kaomoji.replaceAll("\\)", "\\\\)"));
							}
						} else if (kaomoji.contains("}")) {
							input = input.replaceAll(kaomoji.replaceAll("\\}", "\\\\}"), "");
						} else if (kaomoji.contains("(")) {
							input = input.replaceAll(kaomoji.replaceAll("\\(", "\\\\("), "");
						} else if (kaomoji.contains("{")) {
							input = input.replaceAll(kaomoji.replaceAll("\\{", "\\\\{"), "");
						} else if (kaomoji.contains("[")) {
							input = input.replaceAll(kaomoji.replaceAll("\\[", "\\\\["), "");
						} else if (kaomoji.contains("]")) {
							input = input.replaceAll(kaomoji.replaceAll("\\]", "\\\\]"), "");
						} else {
							input = input.replaceAll(kaomoji, "");
						}
						allEmoticonsEtcInTweet.add(kaomoji.trim());
					}
				}
				tweet.setTweet_emoticons(allEmoticonsEtcInTweet);
				for (String url : tweet.getTweet_urls()) {
					input = input.replaceAll(url, "");
				}

				for (String hashtag : tweet.getHashtags()) {
					input = input.replaceAll("#" + hashtag, "");
				}

				for (String mention : tweet.getMentions()) {
					input = input.replaceAll("@" + mention, "");
				}

				input = input.replaceAll(
						"((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
						"");
				if (tweet.getTweet_lang().equals("en")) {
					tweet.setText_en(input);
				} else if (tweet.getTweet_lang().equals("es")) {
					tweet.setText_es(input);
				} else if (tweet.getTweet_lang().equals("tr")) {
					tweet.setText_tr(input);
				} else if (tweet.getTweet_lang().equals("ko")) {
					tweet.setText_ko(input);
				}
				FileUtils.write(outputFile, tweet.getAsJson(tweet) + "\n", true);
			}
		}
	}

}
