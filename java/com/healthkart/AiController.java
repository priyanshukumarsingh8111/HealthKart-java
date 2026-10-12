package com.healthkart;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.healthkart.Util.*;

/** Rule-based AI health agent + symptom triage (same logic as the Node version). */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MED = Pattern.compile(
            "(?:take|for|of)\\s+([a-zA-Z0-9\\s]+?)(?:\\s+at|\\s+after|\\s+before|\\s+in|\\s+dosage|$)",
            Pattern.CASE_INSENSITIVE);

    private final JsonStore store;
    private final Realtime rt;

    public AiController(JsonStore store, Realtime rt) {
        this.store = store;
        this.rt = rt;
    }

    private record Snapshot(List<Map<String, Object>> prescriptions, List<Map<String, Object>> reminders) {}

    private Map<String, Object> saveReminder(Map<String, Object> reminder) {
        store.write(db -> {
            list(db, "reminders").add(0, reminder);
            return null;
        });
        rt.emit("reminders_updated", m("recipientEmail", reminder.get("userEmail")));
        return reminder;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);
        if (!(b.get("message") instanceof String message) || message.isEmpty())
            throw ApiException.of(400, "Message is required");
        String userEmail = s(b.get("userEmail"));

        String text = message.toLowerCase().trim();
        Snapshot snap = store.read(db -> new Snapshot(
                list(db, "prescriptions").stream()
                        .filter(p -> userEmail == null || (s(p.get("patientEmail")) != null
                                && p.get("patientEmail").toString().equalsIgnoreCase(userEmail)))
                        .collect(Collectors.toList()),
                list(db, "reminders").stream()
                        .filter(r -> userEmail == null || (s(r.get("userEmail")) != null
                                && r.get("userEmail").toString().equalsIgnoreCase(userEmail)))
                        .collect(Collectors.toList())));

        String reply;
        String actionTaken = null;
        Map<String, Object> scheduledReminder = null;
        String owner = userEmail != null ? userEmail : "priya@healthkart.com";

        // 1. Natural-language reminder scheduling
        if (has(text, "remind me", "set reminder", "schedule reminder", "add reminder")) {
            boolean isYoga = has(text, "yoga", "pranayama", "asana", "namaskar", "exercise");

            String reminderTime = "08:00";
            Matcher tm = TIME.matcher(text);
            if (tm.find()) {
                int hours = Integer.parseInt(tm.group(1));
                String minutes = tm.group(2) != null ? String.format("%2s", tm.group(2)).replace(' ', '0') : "00";
                String meridian = tm.group(3) != null ? tm.group(3).toLowerCase() : null;
                if ("pm".equals(meridian) && hours < 12) hours += 12;
                if ("am".equals(meridian) && hours == 12) hours = 0;
                reminderTime = String.format("%02d:%s", hours, minutes);
            }

            if (isYoga) {
                String pose = "Daily Wellness Yoga";
                if (text.contains("surya namaskar")) pose = "Surya Namaskar (Sun Salutations)";
                else if (text.contains("pranayama")) pose = "Anulom Vilom Pranayama";
                else if (text.contains("back")) pose = "Bhujangasana (Back Care Yoga)";
                else if (has(text, "sleep", "night")) pose = "Shavasana & Gentle Night Stretch";

                scheduledReminder = saveReminder(m(
                        "id", "rem_" + System.currentTimeMillis() + "_ai_yoga",
                        "type", "yoga",
                        "yogaName", pose,
                        "targetBenefit", "Energy, flexibility and deep respiratory health",
                        "reminderTime", reminderTime,
                        "duration", "15 Mins",
                        "frequency", "Daily",
                        "startDate", today(),
                        "instructions", "Practice on an empty stomach with relaxed, natural breathing.",
                        "userEmail", owner,
                        "status", "ACTIVE",
                        "createdAt", now()));
                actionTaken = "REMINDER_CREATED";
                reply = "🧘 I have scheduled your **" + pose + "** routine for **" + reminderTime
                        + "**! You'll receive real-time audio and on-screen reminders when it's time to practice.";
            } else {
                String medName = "Prescribed Medicine";
                Matcher mm = MED.matcher(text);
                if (mm.find() && mm.group(1).trim().length() > 2) medName = mm.group(1).trim();
                else if (text.contains("paracetamol")) medName = "Paracetamol 500mg";
                else if (text.contains("amoxicillin")) medName = "Amoxicillin 500mg";
                else if (text.contains("vitamin")) medName = "Vitamin D3";
                else if (text.contains("cetirizine")) medName = "Cetirizine 10mg";

                String mealTiming = "After Meal";
                if (text.contains("before breakfast")) mealTiming = "Before Breakfast";
                else if (text.contains("after breakfast")) mealTiming = "After Breakfast";
                else if (text.contains("before lunch")) mealTiming = "Before Lunch";
                else if (text.contains("after lunch")) mealTiming = "After Lunch";
                else if (text.contains("before dinner")) mealTiming = "Before Dinner";
                else if (text.contains("after dinner")) mealTiming = "After Dinner";
                else if (text.contains("empty stomach")) mealTiming = "Empty Stomach";

                scheduledReminder = saveReminder(m(
                        "id", "rem_" + System.currentTimeMillis() + "_ai_med",
                        "type", "medicine",
                        "medicineName", medName,
                        "dosage", "1 Dose",
                        "mealTiming", mealTiming,
                        "reminderTime", reminderTime,
                        "frequency", "Daily",
                        "startDate", today(),
                        "duration", "5 Days",
                        "instructions", "Take " + mealTiming.toLowerCase() + " with a glass of water.",
                        "userEmail", owner,
                        "status", "ACTIVE",
                        "createdAt", now()));
                actionTaken = "REMINDER_CREATED";
                reply = "💊 Scheduled! I've set your medicine reminder for **" + medName + "** at **" + reminderTime
                        + "** (" + mealTiming + "). I will notify you with sound and voice so you never miss eating before/after your medication!";
            }
        }
        // 2. Prescriptions
        else if (has(text, "prescription", "doctor advice", "doctor prescribe")) {
            if (!snap.prescriptions().isEmpty()) {
                Map<String, Object> latest = snap.prescriptions().get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> meds = (List<Map<String, Object>>) latest.getOrDefault("medicines", List.of());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> yogas = (List<Map<String, Object>>) latest.getOrDefault("yogaRoutines", List.of());
                String medsList = meds.stream().map(x -> "• **" + or(x.get("name"), "") + "** (" + or(x.get("dosage"), "")
                        + ") - " + or(x.get("mealTiming"), "After meals")).collect(Collectors.joining("\n"));
                String yogaList = yogas.stream().map(y -> "• **" + or(y.get("poseName"), "") + "** (" + or(y.get("timing"), "Morning")
                        + ") - " + or(y.get("duration"), "")).collect(Collectors.joining("\n"));
                reply = "📋 **Your Latest Prescription by " + latest.get("doctorName") + " (" + latest.get("id") + "):**\n\n**Diagnosis:** "
                        + latest.get("diagnosis") + "\n\n**Medicines to eat:**\n" + medsList + "\n\n**Recommended Yoga:**\n" + yogaList
                        + "\n\n**Doctor's Advice:** " + latest.get("advice");
            } else {
                reply = "You don't have any recorded prescriptions yet. When your doctor conducts a video consultation and issues a prescription, it will appear here automatically with customized medicine eating and yoga reminders!";
            }
        }
        // 3. Schedule / reminders
        else if (has(text, "schedule", "reminder", "what should i take", "today")) {
            List<Map<String, Object>> rem = snap.reminders();
            if (!rem.isEmpty()) {
                String meds = rem.stream().filter(r -> "medicine".equals(r.get("type")))
                        .map(r -> "💊 **" + r.get("reminderTime") + "** - " + r.get("medicineName") + " (" + r.get("dosage")
                                + ") — *" + r.get("mealTiming") + "*").collect(Collectors.joining("\n"));
                String yogas = rem.stream().filter(r -> "yoga".equals(r.get("type")))
                        .map(r -> "🧘 **" + r.get("reminderTime") + "** - " + r.get("yogaName") + " (" + r.get("duration") + ")")
                        .collect(Collectors.joining("\n"));
                reply = "📅 **Your Real-Time Daily Health Schedule:**\n\n**Medicine Eating Timings:**\n"
                        + (meds.isEmpty() ? "None scheduled." : meds)
                        + "\n\n**Daily Yoga & Pranayama:**\n" + (yogas.isEmpty() ? "None scheduled." : yogas)
                        + "\n\n*Our real-time notification engine will chime when each time arrives!*";
            } else {
                reply = "You have no active reminders right now. You can tell me: *\"Remind me to take Paracetamol after lunch at 1:30 PM\"* or *\"Schedule morning yoga at 7:00 AM\"*!";
            }
        }
        // 4. Yoga
        else if (has(text, "yoga", "asana", "pranayama", "exercise")) {
            if (has(text, "back", "spine")) {
                reply = "🧘 **Best Yoga for Back Pain & Posture:**\n\n1. **Bhujangasana (Cobra Pose):** Strengthens spinal column and opens the chest. Hold for 30s.\n2. **Balasana (Child's Pose):** Decompresses lumbar vertebrae and calms the nervous system.\n3. **Marjaryasana-Bitilasana (Cat-Cow):** Restores synovial fluid flow in your spine.\n\n*Tip:* Practice in the morning before eating or in the evening on an empty stomach. Would you like me to schedule a daily reminder for this?";
            } else if (has(text, "digestion", "stomach", "gas", "acid")) {
                reply = "🧘 **Best Yoga for Digestion & Metabolism:**\n\n1. **Vajrasana (Thunderbolt Pose):** The only asana recommended immediately *after* eating. Sit for 5-10 mins to enhance digestive blood flow.\n2. **Pavanamuktasana (Wind-Relieving Pose):** Relieves abdominal bloating and trapped gases.\n3. **Ardha Matsyendrasana (Half Fish Pose):** Massages abdominal organs and stimulates pancreas.\n\nWould you like me to schedule a 10-minute Vajrasana reminder after dinner?";
            } else if (has(text, "stress", "anxiety", "sleep")) {
                reply = "🧘 **Best Yoga for Stress Relief & Sound Sleep:**\n\n1. **Anulom Vilom Pranayama:** 10 mins of alternate nostril breathing balances sympathetic and parasympathetic nervous systems.\n2. **Viparita Karani (Legs-Up-The-Wall):** Inversion that lowers cortisol and induces deep physical relaxation.\n3. **Shavasana (Corpse Pose):** 5-10 mins with mindful progressive muscle relaxation before bed.\n\nShall I set a 9:30 PM evening breathing reminder for you?";
            } else {
                reply = "🧘 **HealthKart Yoga Therapy Intelligence:**\nYoga improves drug bioavailability, hormonal balance, and mental clarity.\n\n• **Morning:** Surya Namaskar (Sun Salutations) for vitality.\n• **Post-Meal:** Vajrasana for digestion.\n• **Evening:** Bhujangasana & Pranayama for back fatigue & peaceful sleep.\n\nTell me your health focus (e.g. *back pain, digestion, stress, thyroid, immunity*) and I will tailor a personalized routine with reminders!";
            }
        }
        // 5. Medicines
        else if (has(text, "paracetamol", "fever", "pain")) {
            reply = "💊 **Paracetamol 500mg Guidance:**\n• **Purpose:** Relieves mild to moderate pain and lowers body temperature in fever.\n• **Meal Timing:** Best taken **after food or a snack** with a full glass of water to avoid gastric irritation.\n• **Dosage:** Typically 500mg every 4 to 6 hours as needed; do not exceed 4,000mg in 24 hours.\n• **Precaution:** Avoid combining with alcohol or other acetaminophen-containing cold remedies.";
        } else if (has(text, "amoxicillin", "antibiotic")) {
            reply = "💊 **Amoxicillin 500mg Guidance:**\n• **Purpose:** Broad-spectrum penicillin antibiotic for bacterial respiratory, ear, or throat infections.\n• **Meal Timing:** Take **at the start of a meal or within 20 minutes after eating** to enhance absorption and prevent nausea.\n• **Crucial Rule:** Complete the full course prescribed by your doctor even if you feel completely cured!\n• **Hydration:** Drink plenty of fluids throughout the day.";
        } else if (has(text, "cetirizine", "allergy", "cold")) {
            reply = "💊 **Cetirizine 10mg Guidance:**\n• **Purpose:** Antihistamine relieving runny nose, sneezing, itchy watery eyes, and hives.\n• **Meal Timing:** Can be taken with or without food; best taken **after dinner before bedtime** as it may cause slight drowsiness.\n• **Safety:** Avoid alcohol and nighttime driving while using Cetirizine.";
        } else if (has(text, "eating", "empty stomach", "before food", "after food")) {
            reply = "🍽️ **Why Medicine Eating Timing Matters:**\n\n• **After Food (Post-Prandial):** NSAIDs, pain relievers (Paracetamol), antibiotics (Amoxicillin) protect the gastric mucosa from acid irritation.\n• **Before Food / Empty Stomach:** Proton pump inhibitors (Pantoprazole, Omeprazole) and thyroid medication (Thyroxine) need gastric acid absence for proper absorption.\n• **With Milk / Healthy Fats:** Fat-soluble vitamins (Vitamin D3, E, A, K) absorb up to 50% better with a meal containing healthy fats.";
        } else {
            reply = "Hello! I am your **HealthKart Real-Time AI Health Agent**. 🌿\n\nI can assist you with:\n1. 💊 **Medicine Eating Reminders:** Setting reminders synchronized with breakfast, lunch, or dinner.\n2. 🧘 **Yoga & Pranayama:** Recommending routines for back pain, digestion, stress, sleep, or stamina.\n3. 📋 **Doctor's Prescriptions:** Explaining dosage instructions and precautions given by your doctor.\n4. ⏰ **Live Alerts:** Providing real-time voice and audio alerts right when it's time.\n\nTry asking me: *\"Remind me to take Paracetamol after lunch at 1:30 PM\"* or *\"What yoga should I do for lower back pain?\"*";
        }

        return m("success", true, "reply", reply, "actionTaken", actionTaken, "scheduledReminder", scheduledReminder);
    }

    @PostMapping("/triage")
    public Map<String, Object> triage(@RequestBody(required = false) Map<String, Object> raw) {
        String symptoms = s(body(raw).get("symptoms"));
        if (symptoms == null || symptoms.trim().isEmpty())
            throw ApiException.of(400, "Please describe your symptoms.");

        String text = symptoms.toLowerCase();
        String specialty, doctorId, urgency, urgencyLevel, summary, yoga;
        List<String> questions;

        if (has(text, "skin", "rash", "acne", "itch", "hair", "dermat")) {
            specialty = "Dermatologist";
            doctorId = "priya";
            urgency = "Mild to Moderate (Clear Video Inspection Recommended)";
            urgencyLevel = "mild";
            summary = "Dermatological presentation: patient reports " + symptoms + ". Visual assessment and topical treatment evaluation advised.";
            questions = List.of(
                    "Could this rash or reaction be caused by diet, cosmetics, or allergens?",
                    "Are topical antihistamines or barrier creams safe for daily use?",
                    "How soon should improvement appear before an in-person follow-up?");
            yoga = "Avoid high-heat hot yoga or heavy sweating until skin inflammation calms.";
        } else if (has(text, "heart", "chest", "palpitation", "bp", "blood pressure", "breathless")) {
            specialty = "Cardiologist";
            doctorId = "amit";
            urgency = "Priority (Comprehensive Tele-Triage & Vitals Review)";
            urgencyLevel = "priority";
            summary = "Cardiovascular / Thoracic symptom report: " + symptoms + ". Doctor will evaluate resting heart rate, BP trends, and lifestyle factors.";
            questions = List.of(
                    "Should I monitor my daily resting BP and pulse in morning and evening?",
                    "Are there specific ECG or lipid profile markers to test?",
                    "What dietary sodium reductions and light walking regimens are safest?");
            yoga = "Practice gentle Anulom Vilom and Shavasana; strictly avoid breath retention (Kumbhaka) and Kapalbhati.";
        } else if (has(text, "bone", "joint", "back", "knee", "spine", "muscle", "ortho")) {
            specialty = "Orthopedic";
            doctorId = "arjun";
            urgency = "Moderate (Musculoskeletal Range-of-Motion & Ergonomics Review)";
            urgencyLevel = "moderate";
            summary = "Musculoskeletal strain / Joint complaint: " + symptoms + ". Focus on spinal biomechanics, posture, and anti-inflammatory care.";
            questions = List.of(
                    "Is this posture-related muscle fatigue or ligament/joint strain?",
                    "Should I apply heat or cold therapy, and when?",
                    "What corrective ergonomic exercises can I perform while working?");
            yoga = "Gentle Marjaryasana (Cat-Cow) and Balasana (Child Pose) recommended; avoid deep forward bends without bending knees.";
        } else if (has(text, "child", "baby", "pediatric", "toddler", "kid")) {
            specialty = "Pediatrician";
            doctorId = "neha";
            urgency = "Priority Pediatric Care (Pediatric Video Consultation)";
            urgencyLevel = "priority";
            summary = "Pediatric symptom report: " + symptoms + ". Child requires age-weight adjusted medication dosage and hydration monitoring.";
            questions = List.of(
                    "What is the precise weight-based dosage for pediatric syrup or drops?",
                    "What red-flag signs should prompt in-person emergency care?",
                    "What hydration fluids or electrolytes are best tolerated?");
            yoga = "Encourage gentle butterfly pose and fun deep breathing games without strain.";
        } else {
            specialty = "General Physician";
            doctorId = "rahul";
            urgency = "Routine (Ideal for Video Consultation)";
            urgencyLevel = "routine";
            summary = "Primary care symptom presentation: " + symptoms + ". Comprehensive systemic evaluation, medication review, and recovery plan indicated.";
            questions = List.of(
                    "What is the recommended dosage and meal timing (before/after food) for any prescribed medicine?",
                    "Are there any side effects or food interactions to watch out for?",
                    "What supportive yoga and hydration practices will accelerate recovery?");
            yoga = "Daily Surya Namaskar and Pranayama support immune vitality and respiratory circulation.";
        }

        final String wanted = doctorId;
        Map<String, Object> doctor = store.read(db -> {
            List<Map<String, Object>> docs = list(db, "doctors");
            return docs.stream().filter(d -> wanted.equals(d.get("id"))).findFirst()
                    .orElse(docs.isEmpty() ? null : docs.get(0));
        });

        Map<String, Object> res = m("success", true, "specialty", specialty, "urgency", urgency,
                "urgencyLevel", urgencyLevel);
        if (doctor != null) res.put("recommendedDoctor", doctor);
        res.put("clinicalSummary", summary);
        res.put("suggestedQuestions", questions);
        res.put("yogaPrecaution", yoga);
        return res;
    }
}
