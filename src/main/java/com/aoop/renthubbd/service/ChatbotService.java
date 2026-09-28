package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.ChatbotAiReply;
import com.aoop.renthubbd.dto.ChatbotListing;
import com.aoop.renthubbd.dto.ChatbotResponse;
import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final String SYSTEM_PROMPT = """
            You are RentHub — a warm, friendly assistant for RentHub BD, a Bangladeshi rental marketplace.
            Speak like a helpful human, not a search engine. Keep replies short (1–3 sentences),
            casual but professional. Use emojis sparingly, only when it feels natural.

            AVAILABLE LISTINGS (only these exist — never invent one):
            {{CATALOG}}

            YOUR JOB
            1. Understand what the user actually wants — greetings, small talk, follow-ups,
               or actual property searches. Ask clarifying questions when the request is vague.
            2. For real searches, pick the BEST matching listing IDs and return them.
               Prefer fewer, more relevant results over many loose ones.
            3. Handle negation strictly: "without Banani", "not in Dhaka", "no parking",
               "except Uttara" — exclude those.
            4. Understand context from the conversation history. "Is this a house or apartment?"
               refers to what you just showed. Follow-ups like "cheaper ones", "what about Uttara",
               "tell me more" refer to the previous turn.
            5. If nothing matches, be honest and suggest a next step. Don't shove unrelated
               listings at the user.
            6. "home", "house", "basha", "বাসা" means RESIDENTIAL (HOUSE, APARTMENT, ROOM, HOSTEL).
               NEVER return commercial or industrial listings for a "home" request.
            7. Rent is monthly in BDT. "30k" = 30000. "under 40k" = max 40000.
            8. Bengali / Banglish: ফ্ল্যাট=flat, বাসা=house, দোকান=shop, অফিস=office, ভাড়া=rent.
            9. If a user says something friendly ("pookie", "how are you", "thanks") —
               reply warmly without showing listings.
            10. For general platform questions ("how do I list my property?", "how do visits work?"),
               answer briefly. Do not show listings.

            EXAMPLES
            user: hi
            assistant: {"reply":"Hey! 😊 What kind of space are you looking for today — an apartment, office, shop, or something else?","recommendedListingIds":[],"intent":"greeting"}

            user: how r u
            assistant: {"reply":"Doing great, thanks for asking! Ready to help you find a rental. What are you looking for?","recommendedListingIds":[],"intent":"greeting"}

            user: pookie
            assistant: {"reply":"Haha, thanks! 😄 Tell me what you need and I'll find it.","recommendedListingIds":[],"intent":"greeting"}

            user: i need a 3 bedroom flat in dhanmondi
            assistant: {"reply":"Got it! Here's a 3-bedroom apartment in Dhanmondi that fits:","recommendedListingIds":[3],"intent":"search"}

            user: is this a house or apartment?
            assistant: {"reply":"That one is a 3-bedroom apartment, not a house. Want me to look for an actual house instead?","recommendedListingIds":[],"intent":"other"}

            user: any office in banani
            assistant: {"reply":"Yes! Here's a modern office in Banani:","recommendedListingIds":[2],"intent":"search"}

            user: any office without banani
            assistant: {"reply":"I don't have any offices outside Banani right now. Want me to check other commercial spaces like shops or showrooms?","recommendedListingIds":[],"intent":"search"}

            user: is there any house in dhanmondi under 20000
            assistant: {"reply":"No houses in Dhanmondi under ৳20,000 right now. The cheapest option I have there is a 3-bedroom apartment at ৳42,000. Want to see it?","recommendedListingIds":[],"intent":"search"}

            user: i need a home
            assistant: {"reply":"Sure! Here are the residential options I have — a house in Uttara and an apartment in Dhanmondi:","recommendedListingIds":[1,3],"intent":"search"}

            user: how do I list my property?
            assistant: {"reply":"Easy! Sign up as an Owner, then use 'Add New Listing' in your dashboard. An admin reviews it before it goes live.","recommendedListingIds":[],"intent":"other"}

            OUTPUT FORMAT — return ONLY valid JSON:
            {"reply":"...","recommendedListingIds":[...],"intent":"greeting"|"search"|"other"}
            """;

    private final ChatbotLlmClient llmClient;
    private final ChatbotContextBuilder contextBuilder;
    private final ChatbotHistoryService historyService;
    private final PropertyRepository propertyRepository;
    private final ObjectMapper objectMapper;

    public ChatbotService(ChatbotLlmClient llmClient,
                          ChatbotContextBuilder contextBuilder,
                          ChatbotHistoryService historyService,
                          PropertyRepository propertyRepository,
                          ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextBuilder = contextBuilder;
        this.historyService = historyService;
        this.propertyRepository = propertyRepository;
        this.objectMapper = objectMapper;
    }

    public ChatbotResponse handle(String userMessage, HttpSession session) {
        ChatbotResponse response = new ChatbotResponse();

        if (userMessage == null || userMessage.isBlank()) {
            response.setReply("Just type what you're looking for and I'll help!");
            response.setError(true);
            response.setSource("validation");
            return response;
        }

        String trimmed = userMessage.trim();

        if (llmClient.isEnabled()) {
            String catalog = contextBuilder.buildCatalog();
            String systemPrompt = SYSTEM_PROMPT.replace("{{CATALOG}}", catalog);

            List<Map<String, String>> conversation = new ArrayList<>();
            conversation.add(turn("system", systemPrompt));
            conversation.addAll(historyService.history(session));
            conversation.add(turn("user", trimmed));

            String rawJson = llmClient.complete(conversation);

            if (rawJson != null) {
                try {
                    ChatbotAiReply ai = objectMapper.readValue(rawJson, ChatbotAiReply.class);
                    String reply = ai.getReply() != null && !ai.getReply().isBlank()
                            ? ai.getReply()
                            : "Hmm, I'm not sure how to answer that. Could you rephrase?";

                    response.setReply(reply);
                    response.setSource("groq");

                    historyService.addUser(session, trimmed);
                    historyService.addAssistant(session, reply);

                    String intent = ai.getIntent() != null ? ai.getIntent() : "other";
                    if ("greeting".equalsIgnoreCase(intent)) {
                        response.setGreeting(true);
                        response.setFound(true);
                        return response;
                    }

                    List<Property> matches = resolveListings(ai.getRecommendedListingIds());
                    if (!matches.isEmpty()) {
                        response.setFound(true);
                        response.setListings(toChatbotListings(matches));
                    } else {
                        response.setFound(false);
                    }
                    return response;
                } catch (Exception e) {
                    System.err.println("[ChatbotService] LLM JSON parse failed: " + e.getMessage());
                    System.err.println("[ChatbotService] raw was: " + rawJson);
                }
            }
        }

        return handleViaRules(trimmed, session);
    }

    private Map<String, String> turn(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private List<Property> resolveListings(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Set<Long> unique = new HashSet<>(ids);
        List<Property> out = new ArrayList<>();
        for (Long id : unique) {
            if (id == null) continue;
            Property p = propertyRepository.findById(id).orElse(null);
            if (p == null || p.getStatus() != ListingStatus.PUBLISHED) continue;
            out.add(p);
        }
        return out;
    }

    private List<ChatbotListing> toChatbotListings(List<Property> properties) {
        List<ChatbotListing> out = new ArrayList<>();
        for (Property p : properties) {
            ChatbotListing l = new ChatbotListing();
            l.setId(p.getId());
            l.setTitle(p.getTitle());
            l.setUrl("/listing/" + p.getId());
            l.setPhoto(p.getPrimaryPhoto());
            l.setCity(p.getCity());
            l.setArea(p.getArea());
            l.setTypeLabel(p.getPropertyType() != null ? p.getPropertyType().getLabel() : null);
            l.setCategoryLabel(p.getCategory() != null ? p.getCategory().getLabel() : null);
            l.setRent(p.getMonthlyRent());
            l.setBedrooms(p.getBedrooms());
            l.setBathrooms(p.getBathrooms());
            l.setSizeSqft(p.getSizeSqft());
            l.setFacilities(new ArrayList<>(p.getFacilities()));
            out.add(l);
        }
        return out;
    }

    private ChatbotResponse handleViaRules(String raw, HttpSession session) {
        ChatbotResponse response = new ChatbotResponse();
        response.setSource("fallback");

        String m = raw.toLowerCase(Locale.ROOT).trim();

        String smallTalk = detectSmallTalk(m);
        if (smallTalk != null) {
            response.setGreeting(true);
            response.setFound(true);
            response.setReply(smallTalk);
            historyService.addUser(session, raw);
            historyService.addAssistant(session, smallTalk);
            return response;
        }

        List<String> typeTokens = new ArrayList<>();
        List<String> excludeTokens = new ArrayList<>();
        List<String> cityTokens = new ArrayList<>();
        List<String> areaTokens = new ArrayList<>();
        Integer bedrooms = null;
        Integer bathrooms = null;
        Double maxRent = null;
        Double minRent = null;

        boolean homeIntent = containsAny(m, "home", "basha", "বাসা");
        if (homeIntent) {
            typeTokens.addAll(List.of("HOUSE", "APARTMENT", "ROOM", "HOSTEL", "FLAT_SHARE"));
        }
        if (containsAny(m, "apartment", "flat", "ফ্ল্যাট")) typeTokens.add("APARTMENT");
        if (containsAny(m, "house", "bungalow")) typeTokens.add("HOUSE");
        if (containsAny(m, "room", "bachelor")) typeTokens.add("ROOM");
        if (containsAny(m, "hostel", "mess")) typeTokens.add("HOSTEL");
        if (m.contains("office")) typeTokens.add("OFFICE");
        if (containsAny(m, "shop", "store", "dukan", "দোকান")) typeTokens.add("SHOP");
        if (m.contains("showroom")) typeTokens.add("SHOWROOM");
        if (containsAny(m, "warehouse", "godown")) typeTokens.add("WAREHOUSE");
        if (m.contains("factory")) typeTokens.add("FACTORY");
        if (m.contains("parking")) typeTokens.add("PARKING");
        if (containsAny(m, "land", "plot")) typeTokens.add("LAND");

        String[] areas = {"dhanmondi", "banani", "gulshan", "uttara", "mirpur",
                "bashundhara", "mohammadpur", "badda", "khilgaon", "motijheel"};
        boolean negatedArea = containsAny(m, "without", "not in", "except", "outside", "ছাড়া");

        for (String area : areas) {
            if (m.contains(area)) {
                String proper = area.substring(0, 1).toUpperCase(Locale.ROOT) + area.substring(1);
                if (negatedArea) excludeTokens.add(proper);
                else areaTokens.add(proper);
            }
        }

        for (String city : List.of("dhaka", "chattogram", "sylhet", "khulna",
                "rajshahi", "barishal", "rangpur", "mymensingh")) {
            if (m.contains(city)) {
                String proper = city.substring(0, 1).toUpperCase(Locale.ROOT) + city.substring(1);
                if (negatedArea) excludeTokens.add(proper);
                else cityTokens.add(proper);
            }
        }

        Matcher bedM = Pattern.compile("(\\d+)\\s*[- ]?\\s*(bed|bedroom|br)\\b").matcher(m);
        if (bedM.find()) bedrooms = Integer.parseInt(bedM.group(1));

        Matcher bathM = Pattern.compile("(\\d+)\\s*[- ]?\\s*(bath|bathroom)\\b").matcher(m);
        if (bathM.find()) bathrooms = Integer.parseInt(bathM.group(1));

        Matcher underM = Pattern.compile(
                        "(?:under|below|less than|max|upto|up to|<|\\u2264)\\s*(?:tk|bdt|\\u09F3)?\\s*([\\d,]+)\\s*(k)?")
                .matcher(m);
        if (underM.find()) {
            double v = Double.parseDouble(underM.group(1).replace(",", ""));
            if (underM.group(2) != null) v *= 1000;
            maxRent = v;
        }

        Matcher overM = Pattern.compile(
                        "(?:over|above|more than|min|at least|>)\\s*(?:tk|bdt|\\u09F3)?\\s*([\\d,]+)\\s*(k)?")
                .matcher(m);
        if (overM.find()) {
            double v = Double.parseDouble(overM.group(1).replace(",", ""));
            if (overM.group(2) != null) v *= 1000;
            minRent = v;
        }

        List<Property> all = propertyRepository
                .findByStatusOrderByCreatedAtDesc(ListingStatus.PUBLISHED);
        List<Property> matches = new ArrayList<>();

        for (Property p : all) {
            if (!typeTokens.isEmpty()
                    && (p.getPropertyType() == null
                    || !typeTokens.contains(p.getPropertyType().name()))) continue;
            if (!cityTokens.isEmpty()
                    && (p.getCity() == null || !cityTokens.contains(p.getCity()))) continue;
            if (!areaTokens.isEmpty()
                    && (p.getArea() == null || !areaTokens.contains(p.getArea()))) continue;
            if (!excludeTokens.isEmpty()) {
                boolean excluded = (p.getArea() != null && excludeTokens.contains(p.getArea()))
                        || (p.getCity() != null && excludeTokens.contains(p.getCity()));
                if (excluded) continue;
            }
            if (bedrooms != null && (p.getBedrooms() == null || p.getBedrooms() < bedrooms)) continue;
            if (bathrooms != null && (p.getBathrooms() == null || p.getBathrooms() < bathrooms)) continue;
            if (maxRent != null && (p.getMonthlyRent() == null || p.getMonthlyRent() > maxRent)) continue;
            if (minRent != null && (p.getMonthlyRent() == null || p.getMonthlyRent() < minRent)) continue;
            matches.add(p);
            if (matches.size() >= 4) break;
        }

        String reply;
        if (matches.isEmpty()) {
            reply = buildEmptyReply(m, all, typeTokens, areaTokens, cityTokens, maxRent);
        } else if (matches.size() == 1) {
            reply = "I found 1 listing that matches:";
        } else {
            reply = "I found " + matches.size() + " listings that might match:";
        }

        response.setReply(reply);
        response.setFound(!matches.isEmpty());
        if (!matches.isEmpty()) response.setListings(toChatbotListings(matches));

        historyService.addUser(session, raw);
        historyService.addAssistant(session, reply);

        return response;
    }

    private String detectSmallTalk(String m) {
        if (m.matches("^(hi|hello|hey|yo|salam|assalam.*|good (morning|evening|afternoon))[!. ]*$"))
            return "Hey! 😊 What kind of space are you looking for today?";
        if (containsAny(m, "how are you", "how r u", "how are u", "kemon acho", "kmn achen"))
            return "Doing great, thanks for asking! What are you looking for?";
        if (containsAny(m, "thank", "thanks", "tnx", "thnx"))
            return "You're welcome! Anything else I can help with?";
        if (containsAny(m, "bye", "goodbye", "see you", "see ya"))
            return "Take care! Come back anytime you need to find a rental. 👋";
        if (containsAny(m, "pookie", "love you", "love u", "cutie", "sweet"))
            return "Haha, thanks! 😄 Now let's find you a place — what are you looking for?";
        if (containsAny(m, "what can you do", "what do you do", "help me", "help"))
            return "I can help you find rentals — apartments, houses, offices, shops, warehouses "
                    + "and more. Just tell me what you need!";
        if (m.length() <= 3 && !m.matches(".*\\d.*"))
            return "Not sure what you mean — try something like "
                    + "\"2-bedroom apartment in Dhanmondi\" or \"office in Banani\".";
        return null;
    }

    private String buildEmptyReply(String m, List<Property> all,
                                   List<String> types, List<String> areas,
                                   List<String> cities, Double maxRent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sorry, I couldn't find anything matching that right now.");

        if (!all.isEmpty()) {
            Property cheapest = null;
            for (Property p : all) {
                if (p.getMonthlyRent() == null) continue;
                if (cheapest == null || p.getMonthlyRent() < cheapest.getMonthlyRent()) cheapest = p;
            }
            if (cheapest != null && maxRent != null && cheapest.getMonthlyRent() > maxRent) {
                sb.append(" The cheapest listing I have is ")
                        .append(cheapest.getTitle())
                        .append(" at ৳")
                        .append(Math.round(cheapest.getMonthlyRent()))
                        .append("/month.");
            } else if (!areas.isEmpty() || !cities.isEmpty() || !types.isEmpty()) {
                sb.append(" Try widening your search — maybe a different area or budget.");
            }
        }

        sb.append(" You can also browse all listings from our Explore section.");
        return sb.toString();
    }

    private boolean containsAny(String source, String... tokens) {
        for (String t : tokens) {
            if (source.contains(t)) return true;
        }
        return false;
    }
}