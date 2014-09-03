class AddDomainsToSections < ActiveRecord::Migration
  def change
    add_column :sections, :domains, :string
  end
end
