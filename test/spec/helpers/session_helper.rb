module SessionHelper
  def with_valid_session
    unless @cookies
      post "/hopsworks/api/auth/login", URI.encode_www_form({ email: 'admin@kth.se', password: "admin"}), { content_type: 'application/x-www-form-urlencoded'}
      @cookies = {"SESSIONID"=> json_body[:sessionID]}
    end
    Airborne.configure do |config|
      config.headers = {:cookies => @cookies, content_type: 'application/json' }
    end
  end

  def reset_session
    Airborne.configure do |config|
      config.headers = {:cookies => {}, content_type: 'application/json' }
    end
  end
end
