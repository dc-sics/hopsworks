module FactoryHelper
  def random_id
    SecureRandom.hex(20)
  end

  def short_random_id
    SecureRandom.hex(4)
  end

  def pr(res)
    res.on_output do |val1, val2|
      # a is the process itself, b is the output
      p val2
    end
    # puts "Exit Status:#{res.exit_status}"
    # puts "Command Executed:#{res.command}"
  end

  def clean_database
    byebug
    # if ENV['RSPEC_SSH'] && ENV['RSPEC_SSH']=="true"
    #   require 'net/ssh'
    #   require 'net/ssh/shell'
    #   Net::SSH::start(ENV['RSPEC_SSH_HOST'], 'root') do |ssh|
    #     ssh.shell do |sh|
    #       sh.execute("cd #{ENV['RSPEC_SSH_USER_DIR']}")
    #       sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS oozie\" ' ")
    #       sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"DROP DATABASE IF EXISTS hopsworks\" ' ")
    #       sh.execute("vagrant ssh -c 'rm -rf /srv/hadoop-2.4.0/tmp/dfs/data' ")
    #       sh.execute("vagrant ssh -c 'sudo  /srv/hadoop-2.4.0/sbin/root-test-drop-and-recreate-hopsworks-db.sh' ")
    #       sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS hopsworks CHARACTER SET latin1\" ' ")
    #       sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh  -e \"CREATE DATABASE IF NOT EXISTS oozie CHARACTER SET latin1\" ' ")
    #       sh.execute("vagrant ssh -c 'sudo cat /srv/glassfish/tables.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    #       sh.execute("vagrant ssh -c 'sudo cat /srv/glassfish/rows.sql | sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=hopsworks' ")
    #       sh.execute("vagrant ssh -c 'sudo /var/lib/mysql-cluster/ndb/scripts/mysql-client.sh --database=oozie <  /srv/oozie/oozie.sql' ")
    #       sh.execute("exit")
    #     end
    #   end
    # else
    #
    # end
  end
end
