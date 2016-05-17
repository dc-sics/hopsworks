module WorkflowHelper
  def create_workflow(id)
    post "/hopsworks/api/project/#{id}/workflows", {name: "workflow_#{short_random_id}"}
    json_body
  end

  def with_valid_workflow(id)
    @workflow = create_workflow(id) if @workflow && @workflow[:projectId] != id
    @workflow ||= create_workflow(id)
  end

  def create_node(project_id, id)
    post "/hopsworks/api/project/#{project_id}/workflows/#{id}/nodes", valid_node_params
    json_body
  end

  def with_valid_node(project_id, id)
    @node = create_node(project_id, id) if @node && @node[:workflowId] != id
    @node ||= create_node(project_id, id)
  end

  def valid_node_params
    {id: random_id, type: "blank-node", data: {}}
  end

  def create_edge(project_id, id)
    post "/hopsworks/api/project/#{project_id}/workflows/#{id}/edges", valid_edge_params(project_id, id)
    json_body
  end

  def with_valid_edge(project_id, id)
    @edge = create_edge(project_id, id) if @edge && @edge[:workflowId] != id
    @edge ||= create_edge(project_id, id)
  end

  def valid_edge_params(project_id, id)
    source = create_node(project_id, id)
    target = create_node(project_id, id)
    {id: random_id, sourceId: source[:id], targetId: target[:id]}
  end
end
