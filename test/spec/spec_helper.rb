require 'airborne'
require 'byebug'
require 'active_record'
ActiveRecord::Base.establish_connection ({
  :adapter => "mysql2",
  :host => "bbc1.sics.se",
  :port => "13007",
  :username => "kthfs",
  :password => "kthfs",
  :database => "hopsworks"})

Dir[File.join(File.dirname(__FILE__), 'factories', '**', '*.rb')].each { |f| require f }

Dir[File.join(File.dirname(__FILE__), 'helpers', '**', '*.rb')].each { |f| require f }

RSpec.configure do |config|
  config.include SessionHelper
  config.include ProjectHelper
  config.include WorkflowHelper
  config.include FactoryHelper
  config.before(:all) { clean_projects }
  config.after(:all) { clean_projects }
end

Airborne.configure do |config|
  config.base_url = 'http://bbc1.sics.se:14007'
  config.headers = { content_type: 'application/json' }
end
