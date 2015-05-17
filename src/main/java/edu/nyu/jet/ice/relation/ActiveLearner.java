package edu.nyu.jet.ice.relation;

import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.Event;
import opennlp.model.EventStream;
import opennlp.model.FileEventStream;

import java.io.*;
import java.util.*;

/**
 * Feature based active learner.
 *
 * @author yhe
 * @version 1.0
 */
public class ActiveLearner {

    public static final int    NON_RELATION_MULTIPLIER = 5;

    public static final String ACTIVE_LEARNING_FILE_PREFIX = "ActiveLearning";

    public static final int    QUERY_SIZE = 15;

    private GISModel currentModel = null;

    private LinkedList<EventItem> eventRepository;

    private Set<EventItem> annotatedRepository;

    private int iteration = 0;

    public List<EventItem> getQueryRepository() {
        return queryRepository;
    }

    private List<EventItem> queryRepository;

    private Map<String, List<EventItem>> pathInstanceMap;

    private String relationName;

    public ActiveLearner() {
        super();
    }

    public ActiveLearner(String relationName,
                         String instanceFileName,
                         List<String> positiveSeedPatterns,
                         List<String> negativeSeedPatterns) throws IOException {
        this.relationName = relationName;
        if (positiveSeedPatterns.size() < 1) return;
        String[] parts = positiveSeedPatterns.get(0).split("--");
        String type1 = parts[0].trim();
        String type2 = parts[2].trim();
        loadEvents(instanceFileName);
        queryRepository = new ArrayList<EventItem>();
        annotatedRepository = new HashSet<EventItem>();
        List<Event> initialTrainingInstances = new ArrayList<Event>();
        for (String positivePattern : positiveSeedPatterns) {
            initialTrainingInstances.addAll(findInstances(positivePattern.trim().replace("\\s+", " "), relationName));
            annotatedRepository.addAll(findEventItemInstances(positivePattern.trim().replace("\\s+", " "), relationName));
        }
        for (String negativePattern : negativeSeedPatterns) {
            initialTrainingInstances.addAll(findInstances(negativePattern.trim().replace("\\s+", " "), EventItem.NOT_RELATION_LABEL));
            annotatedRepository.addAll(findEventItemInstances(negativePattern.trim().replace("\\s+", " "), EventItem.NOT_RELATION_LABEL));
        }
        initialTrainingInstances.addAll(
                findTypeErrors(type1,
                        type2,
                        initialTrainingInstances.size() * NON_RELATION_MULTIPLIER));
        annotatedRepository.addAll(
                findTypeErrorEventInstances(type1,
                        type2,
                        initialTrainingInstances.size() * NON_RELATION_MULTIPLIER));
        //writeFeatureFile(initialTrainingInstances, ACTIVE_LEARNING_FILE_PREFIX + ".0.feats");
        //FileReader datafr = new FileReader(new File(ACTIVE_LEARNING_FILE_PREFIX + ".0.feats"));
        trainAndLoadModelFromEvents(initialTrainingInstances, ACTIVE_LEARNING_FILE_PREFIX + ".0.model.gz");
    }

    protected void trainAndLoadModelFromEvents(List<Event> initialTrainingInstances, String modelFileName) throws IOException {
        writeFeatureFile(initialTrainingInstances, modelFileName + ".features");
        EventStream es = new FileEventStream(new File(modelFileName + ".features"));
        GISModel model = GIS.trainModel(es, 100, 1);
        File outputFile = new File(modelFileName);
        GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
        writer.persist();
        currentModel = (GISModel)new SuffixSensitiveGISModelReader(
                new File(modelFileName)).getModel();
    }

    public void writeFeatureFile(List<Event> events, String featureFileName) throws IOException {
        PrintWriter p = new PrintWriter(new FileWriter(featureFileName));
        for (Event e : events) {
            String[] context = e.getContext();
            for (String c : context) {
                p.print(c);
                p.print(" ");
            }
            p.println(e.getOutcome());
        }
        p.close();
    }

    public List<Event> findTypeErrors(String type1, String type2, int limit) {
        List<Event> results = new ArrayList<Event>();
//        int count = 0;
//        for (EventItem e : eventRepository) {
//            if (count >= limit) break;
//            if (!e.sameTypesAs(type1, type2)) {
//                Event event = new Event(EventItem.NOT_RELATION_LABEL, e.context());
//                results.add(event);
//                count++;
//            }
//        }
        List<EventItem> eventItems = findTypeErrorEventInstances(type1, type2, limit);
        results.addAll(eventItemsToEvents(eventItems));
        return results;
    }

    private List<EventItem> findTypeErrorEventInstances(String type1, String type2, int limit) {
        List<EventItem> results = new ArrayList<EventItem>();
        int count = 0;
        for (EventItem e : eventRepository) {
            if (count >= limit) break;
            if (!e.sameTypesAs(type1, type2)) {
                e.setOutcome(EventItem.NOT_RELATION_LABEL);
                results.add(e);
                count++;
            }
        }
        return results;
    }

    private List<EventItem> findEventItemInstances(String path) {
        List<EventItem> instances = new ArrayList<EventItem>();
        if (pathInstanceMap.containsKey(path)) {
            List<EventItem> eventItems = pathInstanceMap.get(path);
            for (EventItem e : eventItems) {
                instances.add(e);
            }
        }
        return instances;
    }

    private List<EventItem> findEventItemInstances(String path, String outcome) {
        List<EventItem> instances = new ArrayList<EventItem>();
        if (pathInstanceMap.containsKey(path)) {
            List<EventItem> eventItems = pathInstanceMap.get(path);
            for (EventItem e : eventItems) {
                e.setOutcome(outcome);
                instances.add(e);
            }
        }
        return instances;
    }

    private List<Event> findInstances(String path) {
        List<Event> instances = new ArrayList<Event>();
        List<EventItem> eventItemInstances = findEventItemInstances(path);
        instances.addAll(eventItemsToEvents(eventItemInstances));
        return instances;
    }

    private List<Event> findInstances(String path, String outcome) {
        List<Event> instances = new ArrayList<Event>();
        List<EventItem> eventItemInstances = findEventItemInstances(path, outcome);
        instances.addAll(eventItemsToEvents(eventItemInstances));
        return instances;
    }

    private Collection<Event> eventItemsToEvents(Collection<EventItem> eventItems) {
        Collection<Event> result = new ArrayList<Event>();
        for (EventItem eventItem : eventItems) {
            result.add(new Event(eventItem.getOutcome(), eventItem.context()));
        }
        return result;
    }

    private Collection<Event> eventItemsToEvents(Collection<EventItem> eventItems, String outcome) {
        Collection<Event> result = new ArrayList<Event>();
        for (EventItem eventItem : eventItems) {
            result.add(new Event(outcome, eventItem.context()));
        }
        return result;
    }

    public void loadEvents(String fileName) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(fileName));
        String line = null;
        eventRepository = new LinkedList<EventItem>();
        pathInstanceMap = new TreeMap<String, List<EventItem>>();
        while ((line = r.readLine()) != null) {
            EventItem e = EventItem.fromLine(line);
            String path = e.getPath().trim().replace("\\s+", " ");
            if (!pathInstanceMap.containsKey(path)) {
                pathInstanceMap.put(path, new ArrayList<EventItem>());
            }
            pathInstanceMap.get(path).add(e);
            eventRepository.add(EventItem.fromLine(line));
        }
        // Randomize the list. all operations thereafter are sequential
        Collections.shuffle(eventRepository);
    }


    public void selectQueries() {
        for (EventItem e : eventRepository) {
            double[] probs = currentModel.eval(e.context());
            e.setPredictedOutcome(currentModel.getBestOutcome(probs));
            e.setScore(probs[currentModel.getIndex(e.getPredictedOutcome())]);
        }
        Collections.sort(eventRepository);
        int i = 0;
        queryRepository = new ArrayList<EventItem>();
        while (i < QUERY_SIZE && !eventRepository.isEmpty()) {
            queryRepository.add(eventRepository.pop());
            i++;
        }
    }

    public void retrain(Collection<EventItem> annotatedItems) throws IOException {
        annotatedRepository.addAll(annotatedItems);
        List<Event> trainingInstances = new ArrayList<Event>();
        trainingInstances.addAll(eventItemsToEvents(annotatedRepository));
        iteration++;
        trainAndLoadModelFromEvents(trainingInstances, ACTIVE_LEARNING_FILE_PREFIX + "." + iteration + ".model.gz");
    }
}

