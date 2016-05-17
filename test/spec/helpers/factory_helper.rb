module FactoryHelper
  def random_id
    SecureRandom.hex(20)
  end

  def short_random_id
    SecureRandom.hex(4)
  end
end
