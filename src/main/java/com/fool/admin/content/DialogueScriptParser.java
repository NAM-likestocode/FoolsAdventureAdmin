package com.fool.admin.content;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogueScriptParser {
    private static final Pattern NPC_LINE = Pattern.compile("^-->\\s*(.+?)(?:\\s\\+(\\d+))?\\s*$");
    private static final Pattern CHOICE_LINE = Pattern.compile("^<--\\s*\\[([^\\]]+)](?:\\s*->\\s*(\\S+))?\\s*$");
    private static final Pattern LABEL_LINE = Pattern.compile("^:(\\S+)\\s*$");

    private DialogueScriptParser() {
    }

    public static ParseResult parse(String script) {
        if (script == null || script.isBlank()) {
            return ParseResult.failure("EMPTY_SCRIPT");
        }
        if (script.length() > AdminContentConstants.MAX_DIALOGUE_SCRIPT_LENGTH) {
            return ParseResult.failure("SCRIPT_TOO_LONG");
        }

        String[] rawLines = script.split("\\R");
        List<ScriptElement> elements = new ArrayList<>();
        @Nullable String pendingLabel = null;

        for (int lineNumber = 0; lineNumber < rawLines.length; lineNumber++) {
            String line = rawLines[lineNumber].trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher labelMatcher = LABEL_LINE.matcher(line);
            if (labelMatcher.matches()) {
                pendingLabel = labelMatcher.group(1);
                continue;
            }

            Matcher npcMatcher = NPC_LINE.matcher(line);
            if (npcMatcher.matches()) {
                String text = npcMatcher.group(1).trim();
                if (text.isEmpty() || text.length() > AdminContentConstants.MAX_DIALOGUE_LINE_LENGTH) {
                    return ParseResult.failure("INVALID_LINE", lineNumber + 1);
                }
                int delaySeconds = 0;
                if (npcMatcher.group(2) != null) {
                    delaySeconds = Integer.parseInt(npcMatcher.group(2));
                    if (delaySeconds < AdminContentConstants.MIN_LINE_DELAY_SECONDS
                            || delaySeconds > AdminContentConstants.MAX_LINE_DELAY_SECONDS) {
                        return ParseResult.failure("INVALID_DELAY", lineNumber + 1);
                    }
                }
                elements.add(new NpcLineElement(text, delaySeconds * 20, pendingLabel));
                pendingLabel = null;
                continue;
            }

            Matcher choiceMatcher = CHOICE_LINE.matcher(line);
            if (choiceMatcher.matches()) {
                List<ChoiceLineElement> choices = new ArrayList<>();
                choices.add(parseChoiceLine(line, choiceMatcher, lineNumber));
                int nextLine = lineNumber + 1;
                while (nextLine < rawLines.length) {
                    String candidate = rawLines[nextLine].trim();
                    if (candidate.isEmpty()) {
                        nextLine++;
                        continue;
                    }
                    Matcher nextChoice = CHOICE_LINE.matcher(candidate);
                    if (!nextChoice.matches()) {
                        break;
                    }
                    choices.add(parseChoiceLine(candidate, nextChoice, nextLine));
                    lineNumber = nextLine;
                    nextLine++;
                }
                if (choices.size() > AdminContentConstants.MAX_DIALOGUE_CHOICES) {
                    return ParseResult.failure("TOO_MANY_CHOICES", lineNumber + 1);
                }
                elements.add(new ChoiceBlockElement(choices, pendingLabel));
                pendingLabel = null;
                continue;
            }

            return ParseResult.failure("INVALID_SYNTAX", lineNumber + 1);
        }

        if (elements.isEmpty()) {
            return ParseResult.failure("EMPTY_SCRIPT");
        }
        if (elements.size() > AdminContentConstants.MAX_DIALOGUE_NODES) {
            return ParseResult.failure("TOO_MANY_NODES");
        }

        return buildScript(elements);
    }

    private static ChoiceLineElement parseChoiceLine(String line, Matcher matcher, int lineNumber) {
        String label = matcher.group(1).trim();
        if (label.isEmpty() || label.length() > AdminContentConstants.MAX_DIALOGUE_LINE_LENGTH) {
            throw new IllegalStateException("Invalid choice at line " + (lineNumber + 1));
        }
        String targetLabel = matcher.group(2);
        return new ChoiceLineElement(label, targetLabel == null ? null : targetLabel.trim());
    }

    private static ParseResult buildScript(List<ScriptElement> elements) {
        Map<String, String> labelToNodeId = new HashMap<>();
        List<DialogueNode> nodes = new ArrayList<>();

        for (int index = 0; index < elements.size(); index++) {
            String nodeId = nodeId(index);
            ScriptElement element = elements.get(index);
            @Nullable String nextNodeId = index + 1 < elements.size() ? nodeId(index + 1) : null;

            if (element instanceof NpcLineElement npcLine) {
                nodes.add(new DialogueNode(
                        nodeId,
                        DialogueSpeaker.NPC,
                        npcLine.text(),
                        npcLine.delayTicks(),
                        nextNodeId,
                        List.of()
                ));
                if (npcLine.label() != null) {
                    if (labelToNodeId.containsKey(npcLine.label())) {
                        return ParseResult.failure("DUPLICATE_LABEL");
                    }
                    labelToNodeId.put(npcLine.label(), nodeId);
                }
            } else if (element instanceof ChoiceBlockElement choiceBlock) {
                List<DialogueChoice> choices = new ArrayList<>();
                for (ChoiceLineElement choice : choiceBlock.choices()) {
                    choices.add(new DialogueChoice(choice.label(), null));
                }
                nodes.add(new DialogueNode(
                        nodeId,
                        DialogueSpeaker.PLAYER,
                        "",
                        0,
                        null,
                        choices
                ));
                if (choiceBlock.label() != null) {
                    if (labelToNodeId.containsKey(choiceBlock.label())) {
                        return ParseResult.failure("DUPLICATE_LABEL");
                    }
                    labelToNodeId.put(choiceBlock.label(), nodeId);
                }
            }
        }

        for (int index = 0; index < elements.size(); index++) {
            if (!(elements.get(index) instanceof ChoiceBlockElement choiceBlock)) {
                continue;
            }
            DialogueNode node = nodes.get(index);
            @Nullable String defaultTarget = index + 1 < elements.size() ? nodeId(index + 1) : null;
            List<DialogueChoice> resolvedChoices = new ArrayList<>();
            for (ChoiceLineElement choice : choiceBlock.choices()) {
                String targetNodeId;
                if (choice.targetLabel() == null) {
                    targetNodeId = defaultTarget;
                } else {
                    targetNodeId = labelToNodeId.get(choice.targetLabel());
                    if (targetNodeId == null) {
                        return ParseResult.failure("UNKNOWN_LABEL", choice.targetLabel());
                    }
                }
                resolvedChoices.add(new DialogueChoice(choice.label(), targetNodeId));
            }
            nodes.set(index, new DialogueNode(
                    node.id(),
                    node.speaker(),
                    node.text(),
                    node.delayTicks(),
                    node.nextNodeId(),
                    resolvedChoices
            ));
        }

        return ParseResult.success(new DialogueScript(nodeId(0), nodes));
    }

    private static String nodeId(int index) {
        return "n" + index;
    }

    private sealed interface ScriptElement permits NpcLineElement, ChoiceBlockElement {
    }

    private record NpcLineElement(String text, int delayTicks, @Nullable String label) implements ScriptElement {
    }

    private record ChoiceBlockElement(List<ChoiceLineElement> choices, @Nullable String label) implements ScriptElement {
    }

    private record ChoiceLineElement(String label, @Nullable String targetLabel) {
    }

    public record ParseResult(boolean success, @Nullable DialogueScript script, @Nullable String errorCode, @Nullable String errorDetail) {
        public static ParseResult success(DialogueScript script) {
            return new ParseResult(true, script, null, null);
        }

        public static ParseResult failure(String errorCode) {
            return new ParseResult(false, null, errorCode, null);
        }

        public static ParseResult failure(String errorCode, int lineNumber) {
            return new ParseResult(false, null, errorCode, String.valueOf(lineNumber));
        }

        public static ParseResult failure(String errorCode, String detail) {
            return new ParseResult(false, null, errorCode, detail);
        }
    }
}
