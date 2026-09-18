package com.knowledge.agent.model;

/**
 * Business port for text generation. Callers never depend on a model vendor or LangChain4j implementation.
 */
public interface AgentModelService {

    AgentModelResponse generate(AgentModelRequest request);
}
