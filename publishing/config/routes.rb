Rails.application.routes.draw do
  resources :courses do
    collection do
      post 'select'
    end
    member do
      post 'activate'
      post 'deactivate'
    end
  end

  resources :chapters do
    member do
      post 'activate'
      post 'deactivate'
    end
  end

  root to: 'home#index'
end
