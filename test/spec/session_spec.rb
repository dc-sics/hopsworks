require 'spec_helper'

describe 'login' do
  it 'should work with valid params' do
    post "/hopsworks/api/auth/login", URI.encode_www_form({ email: 'admin@kth.se', password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
    expect_json_types(sessionID: :string, status: :string)
    expect_status(200)
  end

  it 'should fail with invalid params' do
    post "/hopsworks/api/auth/login", URI.encode_www_form({ email: 'admin_new@kth.se', password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
    expect_json_types(errorMsg: :string)
    expect_status(401)
  end
end
