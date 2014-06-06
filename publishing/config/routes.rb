Rails.application.routes.draw do
  match 'select_course', to: 'courses#select', via: :post

  resources :chapters do
    member do
      post 'activate'
      post 'deactivate'
      post 'moveup'
      post 'movedown'
    end

    resources :sections do
      member do
        post 'activate'
        post 'deactivate'
        post 'moveup'
        post 'movedown'
        get 'preview'
      end
      resources :subsections do
        collection do
          get 'list'
        end
        member do
          post 'activate'
          post 'deactivate'
          post 'moveup'
          post 'movedown'
        end
      end
    end
  end

  root to: 'home#index'
end
