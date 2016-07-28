describe 'projects' do
  describe "#create" do
    context 'without authentication' do
      before :all do
        reset_session
      end
      it "should fail" do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
        expect_status(500)
      end
    end

    context 'with authentication' do
      before :all do
        with_valid_session
      end
      it 'should work with valid params' do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}", description:"", status: 0, services: ["JOBS","ZEPPELIN"], projectTeam:[], retentionPeriod: ""}
        expect_json(errorMsg: -> (value){ expect(value).to be_empty})
        expect_json_keys(:data)
        expect(JSON.parse(json_body[:data])['id']).to be_kind_of(Integer)
        expect_status(201)
      end

      it 'should fail with invalid params' do
        post "/hopsworks/api/project", {projectName: "project_#{Time.now.to_i}"}
        expect_status(500)
      end
    end
  end

end
